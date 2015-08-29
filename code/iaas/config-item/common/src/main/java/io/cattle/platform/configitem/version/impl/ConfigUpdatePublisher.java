package io.cattle.platform.configitem.version.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.events.ConfigUpdateData;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.version.dao.ConfigItemStatusDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.InitializationTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public class ConfigUpdatePublisher extends NoExceptionRunnable implements InitializationTask {

    private static final DynamicIntProperty RETRY = ArchaiusUtil.getInt("item.wait.for.event.tries");
    private static final DynamicLongProperty TIMEOUT = ArchaiusUtil.getLong("item.wait.for.event.timeout.millis");

    private static final Logger log = LoggerFactory.getLogger(ConfigUpdatePublisher.class);

    @Inject
    ScheduledExecutorService executorService;

    @Inject
    EventService eventService;

    @Inject
    AgentLocator agentLocator;

    @Inject
    ConfigItemStatusDao configItemStatusDao;

    BlockingQueue<WorkItem> requests = new LinkedBlockingQueue<>();
    Map<String, List<WorkItem>> waiters = new HashMap<>();
    Set<String> inFlight = new HashSet<>();
    volatile boolean running = true;

    public ListenableFuture<Event> publish(Client client, ConfigUpdate update) {
        WorkItem item = new WorkItem(client, update);

        if (update.getData() == null || update.getData().getItems().size() == 0) {
            reply(item, null, null);
        } else {
            requests.add(item);
        }

        return item.future;
    }

    @Override
    protected void doRun() throws Exception {
        while (running) {
            WorkItem item = requests.take();
            if (item.exit) {
                break;
            }

            loopBody(item);
        }
    }

    protected void loopBody(WorkItem item) throws Exception {
        if (log.isTraceEnabled()) {
            log.info("**************** Loop Start ****************");
        }

        boolean request = item.response == null && item.t == null;
        if (request) {
            if (log.isTraceEnabled()) {
                log.info("\t=== Processing Request [{}] ===", item.key);
            }
            processRequest(item);
        } else {
            if (log.isTraceEnabled()) {
                log.info("\t=== Processing Response [{}] ===", item.key);
            }
            processDone(item);
        }

        publish();

        if (log.isTraceEnabled()) {
            log.info("\t=== Status ===");
            log.info("\t\tConfig Updates, requests=[{}], waiters=[{}] in flight: {}", requests.size(), waiters.size(), inFlight);
            if (this.waiters.size() > 0 && !request) {
                for (Map.Entry<String, List<WorkItem>> entry : this.waiters.entrySet()) {
                    for (WorkItem workItem : entry.getValue()) {
                        for ( ConfigUpdateItem updateItem: workItem.request.getData().getItems()) {
                            log.info("\t\t\tWaiter resource [{}] {} : {}", entry.getKey(), workItem.hashCode(), updateItem);
                        }
                    }
                }
            }
            log.info("\t=== Done ===");
            log.info("**************** Loop Done ****************");
        }
    }

    protected void publish() {
        Map<String, List<WorkItem>> newWaiters = new HashMap<>();
        for (Map.Entry<String, List<WorkItem>> entry : waiters.entrySet()) {
            List<WorkItem> remaining = new ArrayList<>();

            if (inFlight.contains(entry.getKey())) {
                newWaiters.put(entry.getKey(), entry.getValue());
                continue;
            }

            ConfigUpdate update = null;
            WorkItem lastItem = null;
            Client client = null;
            Map<String, ItemVersion> applied = null;
            Set<String> items = new HashSet<>();

            for (WorkItem item : entry.getValue()) {
                if (item.isExpired()) {
                    reply(item, null, new TimeoutException());
                    continue;
                }

                if (applied == null) {
                    applied = getApplied(item.client);
                }

                if (satisfies(true, item.request, applied)) {
                    reply(item, null, null);
                    continue;
                }

                if (update == null) {
                    ConfigUpdateData data = item.request.getData();
                    update = new ConfigUpdate(item.request.getName(), data.getConfigUrl(),
                            new ArrayList<ConfigUpdateItem>());
                    update.setResourceId(item.request.getResourceId());
                    update.setResourceType(item.request.getResourceType());
                    client = item.client;
                }

                for (ConfigUpdateItem itemUpdate : item.request.getData().getItems()) {
                    if (items.contains(itemUpdate.getName())) {
                        continue;
                    }

                    items.add(itemUpdate.getName());
                    update.getData().getItems().add(itemUpdate);
                }

                remaining.add(item);
                lastItem = item;
            }

            if (update != null) {
                inFlight.add(lastItem.key);
                publishUpdate(lastItem, client, update);
            }

            if (remaining.size() > 0) {
                newWaiters.put(entry.getKey(), remaining);
            }
        }

        this.waiters = newWaiters;
    }

    protected void publishUpdate(final WorkItem item, Client client, ConfigUpdate update) {
        if (log.isTraceEnabled()) {
            log.info("\t=== Publish ===");
            List<String> items = new ArrayList<>();
            for (ConfigUpdateItem updateItem : update.getData().getItems()) {
                items.add(updateItem.getName());
            }
            log.info("\t\tUpdate [{}:{}] {}", update.getResourceType(), update.getResourceId(), items);
        }
        ListenableFuture<? extends Event> future = call(client, update);
        Futures.addCallback(future, new FutureCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                item.response = result;
                requests.add(item);
            }

            @Override
            public void onFailure(Throwable t) {
                item.t = t;
                requests.add(item);
            }
        });
        if (log.isTraceEnabled()) {
            log.info("\t=== Publish Sent ===");
        }
    }

    protected void processDone(WorkItem item) {
        inFlight.remove(item.key);

        List<WorkItem> requests = this.waiters.get(item.key);
        List<WorkItem> unsatifiedRequests = new ArrayList<>();
        Map<String, ItemVersion> applied = getApplied(item.client);


        for (WorkItem requestItem : requests) {
            if (item.t != null || Event.TRANSITIONING_ERROR.equals(item.response.getTransitioning()) ) {
                reply(requestItem, item.response, item.t);
            } else if (satisfies(false, requestItem.request, applied)) {
                reply(requestItem, item.response, null);
            } else {
                unsatifiedRequests.add(requestItem);
            }
        }

        if (unsatifiedRequests.size() == 0) {
            if (log.isTraceEnabled()) {
                log.info("\t\tKey [{}] is done", item.key);
            }
            waiters.remove(item.key);
        } else {
            if (log.isTraceEnabled()) {
                log.info("\t\tKey[{}] is not done, unsatisified [{}]", item.key, unsatifiedRequests.size());
            }
            waiters.put(item.key, unsatifiedRequests);
        }
    }

    private void processRequest(WorkItem item) {
        CollectionUtils.addToMap(this.waiters, item.key, item, ArrayList.class);
    }

    protected EventCallOptions defaultOptions() {
        EventCallOptions options = new EventCallOptions(RETRY.get(), TIMEOUT.get()).withProgress(new EventProgress() {
            @Override
            public void progress(Event event) {
                ConfigItemStatusManagerImpl.logResponse(null, event);
            }
        });

        return options;
    }

    protected ListenableFuture<? extends Event> call(Client client, ConfigUpdate event) {
        EventCallOptions options = defaultOptions();
        if (client.getResourceType() == Agent.class) {
            RemoteAgent agent = agentLocator.lookupAgent(client.getResourceId());
            return agent.call(event, options);
        }

        return eventService.call(event, options);
    }


    protected Map<String, ItemVersion> getApplied(Client client) {
        return configItemStatusDao.getApplied(client);
    }

    protected boolean satisfies(boolean isRequest, ConfigUpdate request, Map<String, ItemVersion> applied) {
        if (request.getData() == null) {
            return true;
        }

        for (ConfigUpdateItem item : request.getData().getItems()) {
            ItemVersion version = applied.get(item.getName());
            if (version == null) {
                if (log.isTraceEnabled() && !isRequest) {
                    log.info("\t\tUnsatified item {} [{}] is not assigned", item.hashCode(), item.getName());
                }
                return false;
            }

            if (item.getRequestedVersion() == null) {
                if (isRequest) {
                    return false;
                } else {
                    continue;
                }
            }

            if (version.getRevision() < item.getRequestedVersion()) {
                if (log.isTraceEnabled() && !isRequest) {
                    log.info("\t\tUnsatified item {} [{}] [{}<{}]", item.hashCode(), item.getName(), version.getRevision(), item.getRequestedVersion());
                }
                return false;
            }
        }

        return true;
    }

    protected void reply(WorkItem item, Event response, Throwable t) {
        if (t != null) {
            item.future.setException(t);
            return;
        }

        EventVO<Object> event = EventVO.reply(item.request);
        if (response != null) {
            EventUtils.copyTransitioning(response, event);
            event.setData(response.getData());
        }

        log.info("\t\tFinished work item {} [{}]", item.key, item.hashCode());
        item.future.set(event);
    }

    @Override
    public void start() {
        running = true;
        executorService.scheduleWithFixedDelay(this, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        running = false;
        requests.add(new WorkItem(true));
    }

    private static final class WorkItem {
        Client client;
        ConfigUpdate request;
        Event response;
        String key;
        SettableFuture<Event> future;
        Throwable t;
        boolean exit;

        public WorkItem(boolean exit) {
            this.exit = exit;
        }

        public WorkItem(Client client, ConfigUpdate request) {
            /* Sync up time so that we can detect timeout */
            this.client = client;
            this.request = request;
            this.key = String.format("%s:%s", request.getResourceType(), request.getResourceId());
            this.future = SettableFuture.create();
        }

        public boolean isExpired() {
            Long timeout = TIMEOUT.get() * RETRY.get();
            return System.currentTimeMillis() > (request.getTime().getTime() + timeout);
        }

    }

}
