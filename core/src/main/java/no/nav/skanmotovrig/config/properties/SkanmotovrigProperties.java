package no.nav.skanmotovrig.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@ToString
@ConfigurationProperties("skanmotovrig")
@Validated
public class SkanmotovrigProperties {

	private final ServiceUserProperties serviceuser = new ServiceUserProperties();
	private final FilomraadeProperties filomraade = new FilomraadeProperties();
	private final Ovrig ovrig = new Ovrig();
	private final SftpProperties sftp = new SftpProperties();
	private final Endpoints endpoints = new Endpoints();
	private final JiraProperties jira = new JiraProperties();


	@Data
	@Validated
	public static class ServiceUserProperties {
		@ToString.Exclude
		@NotEmpty
		private String username;

		@ToString.Exclude
		@NotEmpty
		private String password;
	}

	@Data
	@Validated
	public static class FilomraadeProperties {
		@NotEmpty
		private String inngaaendemappe;

		@NotEmpty
		private String feilmappe;

		@NotEmpty
		private String avstemmappe;
	}

	@Data
	@Validated
	public static class Ovrig {
		@NotEmpty
		private String endpointuri;

		@NotEmpty
		private String endpointconfig;

		@NotEmpty
		private String schedule;

		@NotNull
		private Duration completiontimeout;

		@NotNull
		private final FilomraadeProperties filomraade = new FilomraadeProperties();
	}

	@Data
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

	@Data
	@Validated
	public static class JiraProperties {
		@NotEmpty
		private String username;

		@NotEmpty
		private String password;

		@NotEmpty
		private String url;
	}
	@Data
	@Validated
	public static class Endpoints {
		/**
		 * URL til dokarkiv journalpost api.
		 */
		@NotNull
		private AzureEndpoint dokarkiv;
	}

	@Data
	@Validated
	public static class AzureEndpoint {
		/**
		 * Url til tjeneste som har azure autorisasjon
		 */
		@NotEmpty
		private String url;

		/**
		 * Scope til azure client credential flow
		 */
		@NotEmpty
		private String scope;
	}
}


