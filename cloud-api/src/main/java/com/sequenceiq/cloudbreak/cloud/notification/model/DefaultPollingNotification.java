package com.sequenceiq.cloudbreak.cloud.notification.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

public class DefaultPollingNotification implements PollingNotification<PollingInfo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPollingNotification.class);

    private PollingInfo dummyPollingInfo;

    public DefaultPollingNotification(PollingInfo dummyPollingInfo) {
        this.dummyPollingInfo = dummyPollingInfo;
    }

    @Override
    public PollingInfo pollingInfo() {
        return dummyPollingInfo;
    }

    @Override
    public void operationCompleted(PollingInfo pollingInfo) {
        LOGGER.debug("TODO: Polling operation completed: {}", pollingInfo);
    }

    //BEGIN GENERATED CODE

    @Override
    public String toString() {
        return "DefaultPollingNotification{" +
                "dummyPollingInfo=" + dummyPollingInfo +
                '}';
    }

    //END GENERATED CODE

}
