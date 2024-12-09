package no.nav.skanmotovrig.ovrig.decrypt;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.metrics.DokCounter;
import no.nav.skanmotovrig.ovrig.ErrorMetricsProcessor;
import no.nav.skanmotovrig.ovrig.MdcRemoverProcessor;
import no.nav.skanmotovrig.ovrig.MdcSetterProcessor;
import no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelope;
import no.nav.skanmotovrig.ovrig.PostboksOvrigService;
import no.nav.skanmotovrig.ovrig.PostboksOvrigSkanningAggregator;
import no.nav.skanmotovrig.ovrig.SkanningmetadataCounter;
import no.nav.skanmotovrig.ovrig.SkanningmetadataUnmarshaller;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static no.nav.skanmotovrig.metrics.DokCounter.DOMAIN;
import static no.nav.skanmotovrig.metrics.DokCounter.OVRIG;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.FILE_NAME_PRODUCED;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Slf4j
@Component
public class PostboksOvrigRoutePGPEncrypted extends RouteBuilder {
	private static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
	private static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
	private static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
	private static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
	private static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

	private final SkanmotovrigProperties skanmotovrigProperties;
	private final PostboksOvrigService postboksOvrigService;
	private final PgpDecryptService pgpDecryptService;

	@Autowired
	public PostboksOvrigRoutePGPEncrypted(
			SkanmotovrigProperties skanmotovrigProperties,
			PostboksOvrigService postboksOvrigService,
			PgpDecryptService pgpDecryptService) {
		this.skanmotovrigProperties = skanmotovrigProperties;
		this.postboksOvrigService = postboksOvrigService;
		this.pgpDecryptService = pgpDecryptService;
	}

	@Override
	public void configure() {

		String PGP_AVVIK = "direct:pgp_encrypted_avvik_ovrig";
		String PROCESS_PGP_ENCRYPTED = "direct:pgp_encrypted_process_ovrig";

		// @formatter:off
		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(ERROR, log, "Skanmotovrig feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
				.to(PGP_AVVIK)
				.log(ERROR, log, "Skanmotovrig skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");


		// Får ikke dekryptert .pgp.zip - mest sannsynlig mismatch mellom private key og public key
		onException(PGPException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(ERROR, log, "Skanmotovrig feilet i dekryptering av .zip.pgp for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip.pgp"))
				.to("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.feilmappe}}" +
						"?{{skanmotovrig.ovrig.endpointconfig}}")
				.log(ERROR, log, "Skanmotovrig skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".")
				.end()
				.process(new MdcRemoverProcessor());

		// Kjente funksjonelle feil
		onException(AbstractSkanmotovrigFunctionalException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.process(new ErrorMetricsProcessor())
				.log(WARN, log, "Skanmotovrig feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
				.setHeader(FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
				.to(PGP_AVVIK)
				.log(WARN, log, "Skanmotovrig skrev feiletzip=${header." + FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

		from("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.inngaaendemappe}}" +
				"?{{skanmotovrig.ovrig.endpointconfig}}" +
				"&delay=" + TimeUnit.SECONDS.toMillis(60) +
				"&antInclude=*zip.pgp,*ZIP.pgp" +
				"&initialDelay=1000" +
				"&maxMessagesPerPoll=10" +
				"&move=processed" +
				"&scheduler=spring&scheduler.cron={{skanmotovrig.ovrig.schedule}}")
				.routeId("read_encrypted_PGP_ovrig_zip_from_sftp")
				.log(INFO, log, "Skanmotovrig-pgp starter behandling av fil=${file:absolute.path}.")
				.setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
				.process(exchange -> exchange.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, cleanDotPgpExtension(simple("${file:name.noext.single}"), exchange)))
				.process(new MdcSetterProcessor())
				.bean(pgpDecryptService)
				.split(new ZipSplitter()).streaming()
					.aggregate(simple("${file:name.noext.single}"), new PostboksOvrigSkanningAggregator())
						.completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
						.completionTimeout(skanmotovrigProperties.getOvrig().getCompletiontimeout().toMillis())
						.setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
						.process(new MdcSetterProcessor())
						.process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DOMAIN, OVRIG)))
						.process(exchange -> exchange.getIn().getBody(PostboksOvrigEnvelope.class).validate())
						.bean(new SkanningmetadataUnmarshaller())
						.bean(new SkanningmetadataCounter())
						.setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
						.to(PROCESS_PGP_ENCRYPTED)
					.end() // aggregate
				.end() // split
				.process(new MdcRemoverProcessor())
				.log(INFO, log, "Skanmotovrig behandlet ferdig fil=${file:absolute.path}.");

		from(PROCESS_PGP_ENCRYPTED)
				.routeId(PROCESS_PGP_ENCRYPTED)
				.process(new MdcSetterProcessor())
				.log(INFO, log, "Skanmotovrig behandler " + KEY_LOGGING_INFO + ".")
				.bean(postboksOvrigService)
				.log(INFO, log, "Skanmotovrig journalførte journalpostId=${body}. " + KEY_LOGGING_INFO + ".")
				.process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DOMAIN, OVRIG)))
				.process(new MdcRemoverProcessor());

		from(PGP_AVVIK)
				.routeId("pgp_encrypted_avvik_ovrig")
				.choice().when(body().isInstanceOf(PostboksOvrigEnvelope.class))
				.setBody(simple("${body.createZip}"))
				.to("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.feilmappe}}" +
						"?{{skanmotovrig.ovrig.endpointconfig}}")
				.otherwise()
				.log(ERROR, log, "Skanmotovrig teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
				.end()
				.process(new MdcRemoverProcessor());

		// @formatter:on
	}

	// Input blir .zip siden .pgp er strippet bort
	private String cleanDotPgpExtension(ValueBuilder value1, Exchange exchange) {
		String stringRepresentation = value1.evaluate(exchange, String.class);
		if (stringRepresentation.contains(".zip")) {
			return stringRepresentation.replace(".zip", "");
		}
		return stringRepresentation;
	}
}
