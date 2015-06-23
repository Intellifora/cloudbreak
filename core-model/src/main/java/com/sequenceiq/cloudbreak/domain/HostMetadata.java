package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "HostMetadata.findHostsInCluster",
                query = "SELECT h FROM HostMetadata h "
                        + "WHERE h.hostGroup.cluster.id= :clusterId"),
        @NamedQuery(
                name = "HostMetadata.findHostsInClusterByName",
                query = "SELECT h FROM HostMetadata h "
                        + "WHERE h.hostGroup.cluster.id= :clusterId AND h.hostName = :hostName")
})
public class HostMetadata {

    @Id
    @GeneratedValue
    private Long id;

    private String hostName;

    @ManyToOne
    private HostGroup hostGroup;

    @Enumerated(EnumType.STRING)
    private HostMetadataState hostMetadataState = HostMetadataState.HEALTHY;

    public HostMetadata() {

    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public HostGroup getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    public HostMetadataState getHostMetadataState() {
        return hostMetadataState;
    }

    public void setHostMetadataState(HostMetadataState hostMetadataState) {
        this.hostMetadataState = hostMetadataState;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "hostGroup");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "hostGroup");
    }

}
