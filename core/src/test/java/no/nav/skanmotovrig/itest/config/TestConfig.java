package no.nav.skanmotovrig.itest.config;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.CoreConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SkanmotovrigProperties.class)
@Import(CoreConfig.class)
public class TestConfig {
}
