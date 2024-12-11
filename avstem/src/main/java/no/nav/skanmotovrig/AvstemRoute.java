package no.nav.skanmotovrig;

import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.FilIkkeFunnetException;
import no.nav.skanmotovrig.jira.OpprettJiraService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotovrig.jira.OpprettJiraService.ANTALL_FILER_AVSTEMT;
import static no.nav.skanmotovrig.jira.OpprettJiraService.ANTALL_FILER_FEILET;
import static no.nav.skanmotovrig.mdc.MDCConstants.PROPERTY_AVSTEM_FILNAVN;
import static no.nav.skanmotovrig.utils.LocalDateAdapter.avstemtDato;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.LoggingLevel.ERROR;
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
				.log(WARN, log, "Skanmotovrig feilet å prossessere avstemReference avstemmingsfil. Exception:${exception};" );

		onException(GenericFileOperationFailedException.class)
				.handled(true)
				.process(new MdcSetterProcessor())
				.log(ERROR, log, "Skanmotovrig fant ikke avstemmingstfil for " +  avstemtDato() + ". Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}");


		from("cron:tab?schedule={{skanmotovrig.ovrig.avstemschedule}}")
				.pollEnrich("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.avstemmappe}}" +
				"?{{skanmotovrig.ovrig.endpointconfig}}" +
				"&antInclude=*.txt" +
				"&maxMessagesPerPoll=1" +
				"&move=processed&maxDepth=1&maxMessagesPerPoll=1&repeatCount=1", 1500)
				.routeId("avstem_routeid")
				.autoStartup("{{skanmotovrig.ovrig.avstemstartup}}")
				.choice()
					.when(header(FILE_NAME).isNull())
						.throwException(new FilIkkeFunnetException("Skanmotovrig fant ikke avstemmingstfil for " +  avstemtDato() + ". Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}"))
				.otherwise()
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
					.choice()
					.when(simple("${body}").isNotNull())
						.setProperty(ANTALL_FILER_FEILET, simple("${body.size}"))
						.log(INFO, log, "skanmotovrig fant ${body.size} feilende avstemmingsreferanser")
						.marshal().csv()
						.bean(opprettJiraService)
						.process(new RemoveMdcProcessor())
					.endChoice()
				.end()
				.end()
				.log(INFO, log, "Skanmotovrig behandlet ferdig avstemmingsfil: ${file:name}");


		from("direct:avvik_avstem")
				.routeId("avvik_avstem")
				.throwException(new FilIkkeFunnetException("Skanmotovrig fant ikke avstemmingstfil for " +  avstemtDato() + ". Undersøk tilfellet og evt. kontakt Iron Mountain."))
				.log(ERROR, log, "Skanmotovrig fant ikke avstemmingstfil for " +  avstemtDato() + ". Undersøk tilfellet og evt. kontakt Iron Mountain. Exception:${exception}")
				.end();

		// @formatter:on
	}
}
