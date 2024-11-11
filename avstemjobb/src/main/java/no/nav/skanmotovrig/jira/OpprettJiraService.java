package no.nav.skanmotovrig.jira;

import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import static java.lang.String.format;

@Component
public class OpprettJiraService {

	private static final String DESCRIPTION = "Se vedlegg for en oversikt over manglende avstemmingsreferanser for skannede dokumenter fra skanmotøvrig";
	public static final String SUMMARY = "Skanmotøvrig: Manglende avstemmingsreferanser for skannede dokumenter";
	private static final String SKANMOTOVRIG_JIRA_BRUKER_NAVN = "srvjiradokdistavstemming";
	private final JiraService jiraService;

	public OpprettJiraService(JiraService jiraService) {
		this.jiraService = jiraService;
	}


	@Handler
	public JiraResponse opprettAvstemJiraOppgave(File file) {
		try {
			JiraRequest jiraRequest = mapJiraRequest(file);
			JiraResponse jiraResponse = jiraService.opprettJiraOppgaveVedVedlegg(jiraRequest);
			return JiraResponse.builder()
					.message(jiraResponse.message())
					.jiraIssueKey(jiraResponse.jiraIssueKey())
					.httpStatusCode(jiraResponse.httpStatusCode())
					.build();
		} catch (JiraClientException e) {
			throw new SkanmotovrigFunctionalException("Kan ikke opprette jira oppgave", e);
		}
	}

	private JiraRequest mapJiraRequest(File file) {
		return JiraRequest.builder()
				.summary(format(SUMMARY))
				.description(format(DESCRIPTION))
				.reporterName(SKANMOTOVRIG_JIRA_BRUKER_NAVN)
				.labels(List.of("skanmotøvrig_avvik"))
				.file(file)
				.build();
	}
}
