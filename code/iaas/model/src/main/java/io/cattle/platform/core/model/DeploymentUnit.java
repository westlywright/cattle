/*
 * This file is generated by jOOQ.
*/
package io.cattle.platform.core.model;


import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.annotation.Generated;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
@Entity
@Table(name = "deployment_unit", schema = "cattle")
public interface DeploymentUnit extends Serializable {

    /**
     * Setter for <code>cattle.deployment_unit.id</code>.
     */
    public void setId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.id</code>.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, precision = 19)
    public Long getId();

    /**
     * Setter for <code>cattle.deployment_unit.name</code>.
     */
    public void setName(String value);

    /**
     * Getter for <code>cattle.deployment_unit.name</code>.
     */
    @Column(name = "name", length = 255)
    public String getName();

    /**
     * Setter for <code>cattle.deployment_unit.account_id</code>.
     */
    public void setAccountId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.account_id</code>.
     */
    @Column(name = "account_id", precision = 19)
    public Long getAccountId();

    /**
     * Setter for <code>cattle.deployment_unit.kind</code>.
     */
    public void setKind(String value);

    /**
     * Getter for <code>cattle.deployment_unit.kind</code>.
     */
    @Column(name = "kind", nullable = false, length = 255)
    public String getKind();

    /**
     * Setter for <code>cattle.deployment_unit.uuid</code>.
     */
    public void setUuid(String value);

    /**
     * Getter for <code>cattle.deployment_unit.uuid</code>.
     */
    @Column(name = "uuid", unique = true, nullable = false, length = 128)
    public String getUuid();

    /**
     * Setter for <code>cattle.deployment_unit.description</code>.
     */
    public void setDescription(String value);

    /**
     * Getter for <code>cattle.deployment_unit.description</code>.
     */
    @Column(name = "description", length = 1024)
    public String getDescription();

    /**
     * Setter for <code>cattle.deployment_unit.state</code>.
     */
    public void setState(String value);

    /**
     * Getter for <code>cattle.deployment_unit.state</code>.
     */
    @Column(name = "state", nullable = false, length = 128)
    public String getState();

    /**
     * Setter for <code>cattle.deployment_unit.created</code>.
     */
    public void setCreated(Date value);

    /**
     * Getter for <code>cattle.deployment_unit.created</code>.
     */
    @Column(name = "created")
    public Date getCreated();

    /**
     * Setter for <code>cattle.deployment_unit.removed</code>.
     */
    public void setRemoved(Date value);

    /**
     * Getter for <code>cattle.deployment_unit.removed</code>.
     */
    @Column(name = "removed")
    public Date getRemoved();

    /**
     * Setter for <code>cattle.deployment_unit.remove_time</code>.
     */
    public void setRemoveTime(Date value);

    /**
     * Getter for <code>cattle.deployment_unit.remove_time</code>.
     */
    @Column(name = "remove_time")
    public Date getRemoveTime();

    /**
     * Setter for <code>cattle.deployment_unit.data</code>.
     */
    public void setData(Map<String,Object> value);

    /**
     * Getter for <code>cattle.deployment_unit.data</code>.
     */
    @Column(name = "data", length = 65535)
    public Map<String,Object> getData();

    /**
     * Setter for <code>cattle.deployment_unit.service_index</code>.
     */
    public void setServiceIndex(String value);

    /**
     * Getter for <code>cattle.deployment_unit.service_index</code>.
     */
    @Column(name = "service_index", length = 255)
    public String getServiceIndex();

    /**
     * Setter for <code>cattle.deployment_unit.service_id</code>.
     */
    public void setServiceId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.service_id</code>.
     */
    @Column(name = "service_id", precision = 19)
    public Long getServiceId();

    /**
     * Setter for <code>cattle.deployment_unit.environment_id</code>.
     */
    public void setStackId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.environment_id</code>.
     */
    @Column(name = "environment_id", nullable = false, precision = 19)
    public Long getStackId();

    /**
     * Setter for <code>cattle.deployment_unit.health_state</code>.
     */
    public void setHealthState(String value);

    /**
     * Getter for <code>cattle.deployment_unit.health_state</code>.
     */
    @Column(name = "health_state", length = 255)
    public String getHealthState();

    /**
     * Setter for <code>cattle.deployment_unit.host_id</code>.
     */
    public void setHostId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.host_id</code>.
     */
    @Column(name = "host_id", precision = 19)
    public Long getHostId();

    /**
     * Setter for <code>cattle.deployment_unit.requested_revision_id</code>.
     */
    public void setRequestedRevisionId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.requested_revision_id</code>.
     */
    @Column(name = "requested_revision_id", precision = 19)
    public Long getRequestedRevisionId();

    /**
     * Setter for <code>cattle.deployment_unit.revision_id</code>.
     */
    public void setRevisionId(Long value);

    /**
     * Getter for <code>cattle.deployment_unit.revision_id</code>.
     */
    @Column(name = "revision_id", precision = 19)
    public Long getRevisionId();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface DeploymentUnit
     */
    public void from(io.cattle.platform.core.model.DeploymentUnit from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface DeploymentUnit
     */
    public <E extends io.cattle.platform.core.model.DeploymentUnit> E into(E into);
}
