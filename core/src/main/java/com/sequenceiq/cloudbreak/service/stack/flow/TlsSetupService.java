package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CERT_DIR;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TLS_CERT_FILE;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.util.Base64;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    private static final int SSH_PORT = 22;
    private static final int TLS_SETUP_TIMEOUT = 180;
    private static final int REMOVE_SSH_KEY_TIMEOUT = 5;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackRepository stackRepository;

    @Value("#{'${cb.cert.dir:" + CB_CERT_DIR + "}' + '/' + '${cb.tls.cert.file:" + CB_TLS_CERT_FILE + "}'}")
    private String tlsCertificatePath;

    public void setupTls(CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties) throws CloudbreakException {

        InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);

        LOGGER.info("SSH into gateway node to setup certificates on gateway.");
        final SSHClient ssh = new SSHClient();
        String sshFingerprint = connector.getSSHFingerprint(stack, gateway.getInstanceId());
        ssh.addHostKeyVerifier(sshFingerprint);
        try {
            ssh.connect(gateway.getPublicIp(), SSH_PORT);
            ssh.authPublickey(connector.getSSHUser(), tlsSecurityService.getSshPrivateFileLocation(stack.getId()));
            ssh.newSCPFileTransfer().upload(tlsCertificatePath, "/tmp/cb-client.pem");
            final Session tlsSetupSession = ssh.startSession();
            tlsSetupSession.allocateDefaultPTY();
            String tlsSetupScript = FileReaderUtils.readFileFromClasspath("init/tls-setup.sh");
            tlsSetupScript = tlsSetupScript.replace("$PUBLIC_IP", gateway.getPublicIp());
            final Session.Command tlsSetupCmd = tlsSetupSession.exec(tlsSetupScript);
            tlsSetupCmd.join(TLS_SETUP_TIMEOUT, TimeUnit.SECONDS);
            tlsSetupSession.close();
            final Session changeSshKeySession = ssh.startSession();
            final Session.Command changeSshKeyCmd = changeSshKeySession.exec("echo '" + stack.getCredential().getPublicKey() + "' > ~/.ssh/authorized_keys");
            changeSshKeyCmd.join(REMOVE_SSH_KEY_TIMEOUT, TimeUnit.SECONDS);
            changeSshKeySession.close();
            ssh.newSCPFileTransfer().download("/tmp/server.pem", tlsSecurityService.getCertDir(stack.getId()) + "/ca.pem");
            if (changeSshKeyCmd.getExitStatus() != 0) {
                throw new CloudbreakException(String.format("TLS setup script exited with error code: %s", changeSshKeyCmd.getExitStatus()));
            }
            if (tlsSetupCmd.getExitStatus() != 0) {
                throw new CloudbreakException(String.format("Failed to remove temp SSH key. Error code: %s", changeSshKeyCmd.getExitStatus()));
            }
            Stack stackWithSecurity = stackRepository.findByIdWithSecurityConfig(stack.getId());
            SecurityConfig securityConfig = stackWithSecurity.getSecurityConfig();
            securityConfig.setServerCert(Base64.encodeAsString(tlsSecurityService.readServerCert(stack.getId()).getBytes()));
            securityConfigRepository.save(securityConfig);
        } catch (IOException e) {
            throw new CloudbreakException("Failed to setup TLS through temporary SSH.", e);
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                throw new CloudbreakException(String.format("Couldn't disconnect temp SSH session"), e);
            }
        }
    }
}
