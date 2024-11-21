package no.nav.skanmotovrig;

import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotovrig.jira.OpprettJiraService.ANTALL_FILER_AVSTEMT;
import static no.nav.skanmotovrig.jira.OpprettJiraService.ANTALL_FILER_FEILET;
import static no.nav.skanmotovrig.mdc.MDCConstants.PROPERTY_AVSTEM_FILNAVN;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class AvstemRoute extends RouteBuilder {

	private final AvstemService avstemService;
	private final OpprettJiraService opprettJiraService;

	public AvstemRoute(AvstemService avstemService,
					   OpprettJiraService opprettJiraService) {
		this.avstemService = avstemService;
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
				"&antInclude=*.txt" +
				"&connectTimeout=15000&timeout=15000" +
				"&move=processed" +
				"&scheduler=spring&scheduler.cron={{skanmotovrig.ovrig.avstemschedule}}")
				.routeId("avstem_routeid")
				.autoStartup("{{skanmotovrig.ovrig.avstemstartup}}")
				.log(INFO, log, "Skanmotovrig starter behandling av avstemfil=${file:name}.")
				.setProperty(PROPERTY_AVSTEM_FILNAVN, simple("${file:name}"))
				.process(new MdcSetterProcessor())
				.split(body().tokenize())
				.streaming()
				.aggregationStrategy(new AvstemAggregationStrategy())
				.convertBodyTo(Set.class)
				.end()
				.setProperty(ANTALL_FILER_AVSTEMT, simple("${body.size}"))
				.log(INFO, log, "hentet ${body.size} avstemmingReferanser fra sftp server")
				.bean(avstemService)
				.setProperty(ANTALL_FILER_FEILET, simple("${body.size}"))
				.log(INFO, log, "skanmotovrig fant ${body.size} feilende avstemmingsreferanser")
				.marshal().csv()
				.bean(opprettJiraService)
				.process(new RemoveMdcProcessor())
				.end();

		// @formatter:on
	}
}
