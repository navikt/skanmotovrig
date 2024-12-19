package no.nav.skanmotovrig;

import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.jira.OpprettJiraService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotovrig.jira.OpprettJiraService.ANTALL_FILER_AVSTEMT;
import static no.nav.skanmotovrig.jira.OpprettJiraService.ANTALL_FILER_FEILET;
import static no.nav.skanmotovrig.jira.OpprettJiraService.avstemmingsfilDato;
import static no.nav.skanmotovrig.mdc.MDCConstants.AVSTEMMINGSFIL_NAVN;
import static no.nav.skanmotovrig.mdc.MDCConstants.AVSTEMT_DATO;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class AvstemRoute extends RouteBuilder {

	private static final int CONNECTION_TIMEOUT = 1500;
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
				.log(WARN, log, "Skanmotovrig feilet å prossessere avstemmingsfil. Exception:${exception};" );

		onException(GenericFileOperationFailedException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotovrig fant ikke avstemmingstfil for ${exchangeProperty." + AVSTEMT_DATO + "}. Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");


		from("cron:tab?schedule={{skanmotovrig.avstem.schedule}}")
				.pollEnrich("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.avstemmappe}}" +
						"?{{skanmotovrig.ovrig.endpointconfig}}" +
						"&antInclude=*.txt,*.TXT" +
						"&maxMessagesPerPoll=1" +
						"&move=processed", CONNECTION_TIMEOUT)
				.routeId("avstem_routeid")
				.autoStartup("{{skanmotovrig.avstem.avstemstartup}}")
				.log(INFO, log, "Skanmotovrig starter cron jobb for å avstemme referanser...")
				.process(new MdcSetterProcessor())
				.process(exchange -> exchange.setProperty(AVSTEMT_DATO, avstemmingsfilDato()))
				.choice()
					.when(header(FILE_NAME).isNull())
						.log(ERROR, log, "Skanmotovrig fant ikke avstemmingsfil for ${exchangeProperty." + AVSTEMT_DATO + "}. Undersøk tilfellet og evt. ser opprettet Jira-sak.")
						.bean(opprettJiraService)
						.log(INFO, log, "Skanmotovrig opprettet jira-sak med key=${body.jiraIssueKey} for manglende avstemmingsfil.")
				.otherwise()
					.log(INFO, log, "Skanmotovrig starter behandling av avstemmingsfil=${file:name}.")
					.setProperty(AVSTEMMINGSFIL_NAVN, simple("${file:name}"))
					.split(body().tokenize())
					.streaming()
						.aggregationStrategy(new AvstemAggregationStrategy())
						.convertBodyTo(Set.class)
					.end()
					.setProperty(ANTALL_FILER_AVSTEMT, simple("${body.size}"))
					.log(INFO, log, "hentet ${body.size} avstemmingReferanser fra sftp server")
					.bean(avstemService)
					.choice()
						.when(simple("${body}").isNotNull())
							.setProperty(ANTALL_FILER_FEILET, simple("${body.size}"))
							.log(INFO, log, "skanmotovrig fant ${body.size} feilende avstemmingsreferanser")
							.marshal().csv()
							.bean(opprettJiraService)
							.log(INFO, log, "Har opprettet Jira-sak=${body.jiraIssueKey} for feilende skanmotovrig avstemmingsreferanser")
							.process(new RemoveMdcProcessor())
					.endChoice()
				.endChoice()
				.end()
				.log(INFO, log, "Skanmotovrig behandlet ferdig avstemmingsfil: ${file:name}");
		// @formatter:on
	}
}
