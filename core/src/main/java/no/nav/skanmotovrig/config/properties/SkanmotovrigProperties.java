package no.nav.skanmotovrig.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import no.nav.dok.validators.Exists;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@ToString
@ConfigurationProperties("skanmotovrig")
@Validated
public class SkanmotovrigProperties {

	@NotBlank
	String slackVarselCron;

	private final FilomraadeProperties filomraade = new FilomraadeProperties();
	@Valid
	private final Ovrig ovrig = new Ovrig();
	@Valid
	private final Avstem avstem = new Avstem();
	@Valid
	private final SftpProperties sftp = new SftpProperties();
	@Valid
	private final Endpoints endpoints = new Endpoints();
	@Valid
	private final JiraConfigProperties jira = new JiraConfigProperties();
	@Valid
	private final Pgp pgp = new Pgp();

	@Data
	@Validated
	public static class FilomraadeProperties {
		@NotBlank
		private String inngaaendemappe;

		@NotBlank
		private String feilmappe;

		@NotBlank
		private String avstemmappe;
	}

	@Data
	@Validated
	public static class Ovrig {
		@NotBlank
		private String endpointuri;

		@NotBlank
		private String endpointconfig;

		@NotBlank
		private String schedule;

		@NotNull
		private Duration completiontimeout;

		@NotNull
		private final FilomraadeProperties filomraade = new FilomraadeProperties();
	}

	@Data
	@Validated
	public static class Avstem {
		@NotBlank
		private String schedule;

		private boolean startup;

	}

	@Data
	@Validated
	public static class SftpProperties {
		@ToString.Exclude
		@NotBlank
		private String host;

		@NotBlank
		@Exists
		private String privateKey;

		@NotBlank
		@Exists
		private String hostKey;

		@ToString.Exclude
		@NotBlank
		private String username;

		@ToString.Exclude
		@NotBlank
		private String port;
	}

	@Data
	@Validated
	public static class JiraConfigProperties {
		@NotBlank
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
		@NotBlank
		private String url;

		/**
		 * Scope til azure client credential flow
		 */
		@NotBlank
		private String scope;
	}

	@Data
	@Validated
	public static class Pgp {
		/**
		 * passphrase for PGP-tjeneste
		 */
		@NotBlank
		@ToString.Exclude
		private String passphrase;

		/**
		 * privateKey for PGP-tjeneste
		 */
		@NotBlank
		@Exists
		private String privateKey;
	}
}


