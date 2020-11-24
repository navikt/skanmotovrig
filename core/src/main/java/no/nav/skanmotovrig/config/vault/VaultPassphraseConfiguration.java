package no.nav.skanmotovrig.config.vault;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.annotation.VaultPropertySource;

@Configuration
@VaultPropertySource(
        value = "${skanmotovrig.vault.secretpath}",
        propertyNamePrefix = "skanmotovrig.secret.",
        ignoreSecretNotFound = false
)
@ConditionalOnProperty("spring.cloud.vault.enabled")
public class VaultPassphraseConfiguration {

}
