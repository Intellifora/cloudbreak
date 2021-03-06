package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.MUNCHAUSEN;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class MunchausenBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MunchausenBootstrap.class);

    private final DockerClient docker;
    private final String[] cmd;
    private final String containerName;

    public MunchausenBootstrap(DockerClient docker, String containerName, String[] cmd) {
        this.docker = docker;
        this.cmd = cmd;
        this.containerName = containerName;
    }

    @Override
    public Boolean call() throws Exception {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(containerName)
                .withName(MUNCHAUSEN.getName() + new Date().getTime())
                .withHostConfig(hostConfig)
                .withCmd(cmd));
        DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock"))));
        LOGGER.info("Munchausen bootstrap container started.");
        return true;
    }

}
