package no.nav.skanmotovrig.itest;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.AvstemConfig;
import no.nav.skanmotovrig.CoreConfig;
import no.nav.skanmotovrig.azure.AzureProperties;
import no.nav.skanmotovrig.azure.OAuthEnabledWebClientConfig;
import no.nav.skanmotovrig.config.properties.JiraAuthProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.properties.SlackProperties;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.ClassLoadableResourceKeyPairProvider;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;

@Slf4j
@EnableAutoConfiguration
@EnableConfigurationProperties({
		SkanmotovrigProperties.class,
		SlackProperties.class,
		JiraAuthProperties.class,
		AzureProperties.class
})
@Import({
		AvstemTestConfig.Config.class,
		OAuthEnabledWebClientConfig.class,
		AvstemTestConfig.SshdSftpServerConfig.class,
		AvstemConfig.class
})
public class AvstemTestConfig {

	@Configuration
	static class Config {
		@Bean
		@Primary
		@Profile("virkedag")
		Clock forrigeDagVirkedagClock() {
			Instant fixedInstant = Instant.parse("2025-08-01T10:00:00Z");
			return Clock.fixed(fixedInstant, CoreConfig.NORGE_ZONE);
		}

		@Bean
		@Primary
		@Profile("fridag")
		Clock forrigeDagFridagClock() {
			Instant fixedInstant = Instant.parse("2025-05-18T10:00:00Z");
			return Clock.fixed(fixedInstant, CoreConfig.NORGE_ZONE);
		}
	}

	@Configuration
	static class SshdSftpServerConfig {
		@Bean
		public Path sshdPath() throws IOException {
			return Files.createTempDirectory("sshd");
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		public SshServer sshServer(
				final Path sshdPath,
				SkanmotovrigProperties skanmotovrigProperties
		) {
			SshServer sshd = SshServer.setUpDefaultServer();
			sshd.setPort(parseInt(skanmotovrigProperties.getSftp().getPort()));
			sshd.setKeyPairProvider(new ClassLoadableResourceKeyPairProvider("sftp/server_id_rsa"));
			sshd.setCommandFactory(new ScpCommandFactory());
			sshd.setSubsystemFactories(singletonList(new SftpSubsystemFactory()));
			// aksepterer alle public keys som presenteres, behøver ikke authorized_keys
			sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
			sshd.setUserAuthFactories(singletonList(new UserAuthNoneFactory()));
			sshd.setFileSystemFactory(new VirtualFileSystemFactory(sshdPath));

			return sshd;
		}
	}
}
