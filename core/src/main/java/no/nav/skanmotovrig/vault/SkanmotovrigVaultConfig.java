package no.nav.skanmotovrig.vault;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.config.properties.SkanmotovrigVaultProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;

@Slf4j
@AllArgsConstructor
@Configuration
@ConditionalOnProperty(value = "spring.cloud.vault.enabled")
public class SkanmotovrigVaultConfig implements InitializingBean {


    public static final String SKANMOTOVRIG_PASSPHRASE = "skantmotovrig.vault.passphrase";

    private final SecretLeaseContainer leaseContainer;
    private final VaultProperties vaultProperties;
    private final SkanmotovrigVaultProperties skanmotovrigVaultProperties;

    @Override
    public void afterPropertiesSet() {

        log.info("Vault verdier name={}, token={}, role={}",
                vaultProperties.getApplicationName(),vaultProperties.getToken(), vaultProperties.getAppRole());

        RequestedSecret secret = RequestedSecret.rotating(skanmotovrigVaultProperties.getSecretpath());

        leaseContainer.addLeaseListener(leaseEvent -> {
            if(leaseEvent.getSource().equals(secret) && leaseEvent instanceof SecretLeaseCreatedEvent) {
                log.info("Rotating secret for path={}", leaseEvent.getSource().getPath());
                SecretLeaseCreatedEvent secretLeaseCreatedEvent = (SecretLeaseCreatedEvent) leaseEvent;
                String passphrase =secretLeaseCreatedEvent.getSecrets().get("passphrase").toString();
                log.info("The passphrase:" + passphrase);
                System.setProperty(SKANMOTOVRIG_PASSPHRASE, passphrase);
            }
        });
        leaseContainer.addRequestedSecret(secret);
    }



}
