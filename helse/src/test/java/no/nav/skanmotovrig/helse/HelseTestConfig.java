package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostConsumer;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.STSConsumer;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SkanmotovrigProperties.class)
@Import({OpprettJournalpostConsumer.class, STSConsumer.class, OpprettJournalpostService.class, HelseTestConfig.SshdSftpServerConfig.class,
        HelseConfig.class})
public class HelseTestConfig {

    private static final String sftpPort = String.valueOf(ThreadLocalRandom.current().nextInt(2000, 2999));

    static {
        System.setProperty("skanmotovrig.sftp.port", sftpPort);
    }

    @Configuration
    static class SshdSftpServerConfig {
        @Bean
        public Path sshdPath() throws IOException {
            return Files.createTempDirectory("sshd");
        }

        @Bean(initMethod = "start", destroyMethod = "stop")
        public SshServer sshServer(final Path sshdPath,
                final SkanmotovrigProperties skanmotovrigProperties) throws IOException {
            SshServer sshd = SshServer.setUpDefaultServer();
            sshd.setPort(Integer.parseInt(skanmotovrigProperties.getSftp().getPort()));
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("src/test/resources/sftp/itest.ser")));
            sshd.setCommandFactory(new ScpCommandFactory());
            sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
            sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get("src/test/resources/sftp/itest_valid.pub")));
            sshd.setUserAuthFactories(Collections.singletonList(new UserAuthNoneFactory()));
            sshd.setFileSystemFactory(new VirtualFileSystemFactory(sshdPath));
            return sshd;
        }
    }
}
