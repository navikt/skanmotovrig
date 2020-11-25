package no.nav.skanmotovrig.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@ConfigurationProperties("skanmotovrig.vault")
@Validated
public class SkanmotovrigVaultProperties {

    @NotBlank
    private String secretpath;

    @NotBlank
    private String backend;

    @NotBlank
    private String kubernetespath;

    @NotBlank
    private String defaultcontext;
}
