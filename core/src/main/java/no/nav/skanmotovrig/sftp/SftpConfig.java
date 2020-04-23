package no.nav.skanmotovrig.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Slf4j
@Configuration
public class SftpConfig {
    @Bean()
    Sftp sftp(
            @Value("${skanmotovrig_sftp_host}") String sftpHost,
            @Value("${skanmotovrig_sftp_privatekey}") String privateKey,
            @Value("${skanmotovrig_sftp_publickey}") String publicKey,
            @Value("${skanmotovrig_sftp_username}") String sftpUsername,
            @Value("${skanmotovrig_sftp_port}") String sftpPort
    ) throws JSchException {
        try {
            return new Sftp(sftpHost, sftpUsername, sftpPort, privateKey, publicKey);
            /*JSch jsch = new JSch();
            Session jschSession = jsch.getSession(sftpUsername, sftpHost, Integer.parseInt(sftpPort));
            jschSession.setPassword(sftpPassword);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no"); // TODO fix proper
            jschSession.setConfig(config);

            return new Sftp(jschSession);
             */
        } catch (Exception e) {
            log.error("Failed to initialize SFTP");
            throw e;
        }
    }
}
