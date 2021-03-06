package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class StackStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartService.class);

    @Inject
    private StackRepository stackRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public FlowContext start(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        Stack stack = stackRepository.findOneWithLists(stackStatusUpdateContext.getStackId());
        cloudPlatformConnectors.get(stack.cloudPlatform()).startAll(stack);
        return stackStatusUpdateContext;
    }
}
