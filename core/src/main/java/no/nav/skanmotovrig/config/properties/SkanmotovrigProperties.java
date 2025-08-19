package no.nav.skanmotovrig.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
	public static class Avstem {
		@NotEmpty
		private String schedule;

		private boolean startup;

	}

	@Data
	@Validated
	public static class SftpProperties {
		@ToString.Exclude
		@NotEmpty
		private String host;

		@NotEmpty
		@Exists
		private String privateKey;

		@NotEmpty
		@Exists
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
	public static class JiraConfigProperties {
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

	@Data
	@Validated
	public static class Pgp {
		/**
		 * passphrase for PGP-tjeneste
		 */
		@NotEmpty
		@ToString.Exclude
		private String passphrase;

		/**
		 * privateKey for PGP-tjeneste
		 */
		@NotEmpty
		@Exists
		private String privateKey;
	}
}


