package no.nav.skanmotovrig.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("skanmotovrig")
@Validated
public class SkanmotovrigProperties {

    @NotNull
    private String dokarkivjournalposturl;

    @NotNull
    private String stsurl;

    @NotNull
    private ServiceUserProperties serviceuser;

    @NotNull
    private FilomraadeProperties filomraade;

    @NotNull
    private FilomraadeProperties helse;

    @NotNull
    private SftpProperties sftp;
}


