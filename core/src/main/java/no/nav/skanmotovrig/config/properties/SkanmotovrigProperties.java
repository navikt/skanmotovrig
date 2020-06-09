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

    private final ServiceUserProperties serviceuser = new ServiceUserProperties();
    private final FilomraadeProperties filomraade = new FilomraadeProperties();
    private final Ovrig ovrig = new Ovrig();
    private final Helse helse = new Helse();
    private final SftpProperties sftp = new SftpProperties();

    @Getter
    @Setter
    @Validated
    public static class ServiceUserProperties {
        @ToString.Exclude
        @NotEmpty
        private String username;

        @ToString.Exclude
        @NotEmpty
        private String password;
    }

    @Getter
    @Setter
    @Validated
    public static class FilomraadeProperties {
        @NotEmpty
        private String inngaaendemappe;

        @NotEmpty
        private String feilmappe;
    }

    @Getter
    @Setter
    @Validated
    public static class Ovrig {
        @NotEmpty
        private String schedule;

        @NotNull
        private final FilomraadeProperties filomraade = new FilomraadeProperties();
    }

    @Getter
    @Setter
    @Validated
    public static class Helse {
        @NotEmpty
        private String endpointuri;

        @NotEmpty
        private String endpointconfig;

        @NotEmpty
        private String schedule;

        @NotNull
        private final FilomraadeProperties filomraade = new FilomraadeProperties();
    }

    @Getter
    @Setter
    @Validated
    public static class SftpProperties {
        @ToString.Exclude
        @NotEmpty
        private String host;

        @ToString.Exclude
        @NotEmpty
        private String privateKey;

        @ToString.Exclude
        @NotEmpty
        private String hostKey;

        @ToString.Exclude
        @NotEmpty
        private String username;

        @ToString.Exclude
        @NotEmpty
        private String port;
    }
}


