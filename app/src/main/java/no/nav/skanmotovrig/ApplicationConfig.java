package no.nav.skanmotovrig;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigVaultProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;

@ComponentScan
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties({
		SkanmotovrigProperties.class,
		SkanmotovrigVaultProperties.class
})
@EnableRetry
@Configuration
public class ApplicationConfig {

}
