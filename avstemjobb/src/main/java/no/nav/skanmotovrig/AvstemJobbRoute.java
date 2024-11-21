package no.nav.skanmotovrig;

import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class AvstemJobbRoute extends RouteBuilder {

	private final AvstemJobbService avstemJobbService;
	private final OpprettJiraService opprettJiraService;

	public AvstemJobbRoute(AvstemJobbService avstemJobbService,
						   OpprettJiraService opprettJiraService) {
		this.avstemJobbService = avstemJobbService;
		this.opprettJiraService = opprettJiraService;
	}

	@Override
	public void configure() {

		// @formatter:off

		onException(Exception.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.log(WARN, log, "Skanmotovrig feilet teknisk, Exception:${exception}");

		onException(AbstractSkanmotovrigFunctionalException.class, JiraClientException.class)
				.handled(true)
				.useOriginalMessage()
				.log(WARN, log, "Skanmotøvrig feilet å prossessere avstemReference fil. Exception:${exception};" );


		from("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.avstemmappe}}" +
				"?{{skanmotovrig.ovrig.endpointconfig}}" +
				"&delay=" + TimeUnit.SECONDS.toMillis(60) +
				"&antInclude=*.txt,*.TXT" +
				"&initialDelay=1000" +
				"&move=processed" +
				"&jailStartingDirectory=false"+
				"&scheduler=spring&scheduler.cron={{skanmotovrig.ovrig.avstemschedule}}")
				.routeId("avstem_routeid")
				.log(INFO, log, "Skanmotovrig starter behandling av avstemfil=${file:name}.")
				.process(new MdcSetterProcessor())
				.split(body().tokenize())
				.streaming()
					.aggregationStrategy(new AvstemmingReferanserAggregationStrategy())
				.convertBodyTo(Set.class)
				.end()
				.choice().when(body().isNotNull())
					.log(INFO, log, "hentet avstemmingReferanser=${body} fra sftp server")
					.bean(avstemJobbService)
					.log(INFO, log, "avstemJobb fant ${body.size} feilende avstemmingReferanser: ${body}")
					.marshal().csv()
					.setHeader(FILE_NAME, simple("feilende-skanmøtovrig-avstemreferenser-${date:now:yyyyMMddHHmmss}.cvs"))
					.process(exchange -> {
						// Skriver marshaled CSV byte stream til et midlertidig fil
						byte[] csvBytes = exchange.getIn().getBody(byte[].class);
						File tempFile = File.createTempFile("Skanmotovrig-FeilendeAvstemmingReferanser-" + LocalDate.now(), ".csv");
						try (FileOutputStream fos = new FileOutputStream(tempFile)) {
							fos.write(csvBytes);
						}
						exchange.getIn().setBody(tempFile);
					})
					.bean(opprettJiraService)
				.endChoice()
				.end();

		// @formatter:on
	}
}
