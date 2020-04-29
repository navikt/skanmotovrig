package no.nav.skanmotovrig.sftp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SftpConfig {
    @Bean()
    Sftp sftp(
            @Value("${skanmotovrig_sftp_host}") String sftpHost,
            @Value("${skanmotovrig_sftp_privatekey}") String privateKey,
            @Value("${skanmotovrig_sftp_hostkey}") String hostKey,
            @Value("${skanmotovrig_sftp_username}") String sftpUsername,
            @Value("${skanmotovrig_sftp_port}") String sftpPort
    ) {
        return new Sftp(sftpHost, sftpUsername, sftpPort, privateKey, hostKey);
    }
}
