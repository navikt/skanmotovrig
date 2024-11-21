package no.nav.skanmotovrig;

import no.nav.skanmotovrig.azure.AzureProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigVaultProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@ComponentScan
@EnableConfigurationProperties({
		SkanmotovrigProperties.class,
		SkanmotovrigVaultProperties.class,
		AzureProperties.class
})
@EnableRetry
@Configuration
public class ApplicationConfig {

}
