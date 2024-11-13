package no.nav.skanmotovrig.jira;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.String.format;

@Slf4j
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
	public JiraResponse opprettAvstemJiraOppgave(byte[] csvByte) {
		File file = createFile(csvByte);
		try {
			if (!file.exists()) {
				log.warn("fant ikke feilende avstemmingsfil og kan ikke opprette jira oppgave");
				return null;
			}
			JiraRequest jiraRequest = mapJiraRequest(file);
			JiraResponse jiraResponse = jiraService.opprettJiraOppgaveVedVedlegg(jiraRequest);
			log.info("opprettet jira oppgave for feilende skanmotovrig avstemmingsreferanser med jira-sak={}", jiraResponse.jiraIssueKey());
			return JiraResponse.builder()
					.message(jiraResponse.message())
					.jiraIssueKey(jiraResponse.jiraIssueKey())
					.httpStatusCode(jiraResponse.httpStatusCode())
					.build();
		} catch (JiraClientException e) {
			throw new SkanmotovrigFunctionalException("kan ikke opprette jira oppgave", e);
		}
	}

	private File createFile(byte[] csvByte) {
		try {
			DateTimeFormatter dateTimeFormatters = DateTimeFormatter.ISO_LOCAL_DATE;
			File tempFile = File.createTempFile("skanmotovrig-feilende-avstemming-" + LocalDateTime.now().format(dateTimeFormatters), ".csv");
			FileOutputStream fs = new FileOutputStream(tempFile);
			fs.write(csvByte);
			return tempFile;
		} catch (IOException ex) {
			throw new SkanmotovrigFunctionalException("I/O feil med feilmelding=" + ex.getMessage(), ex);
		}
	}

	private JiraRequest mapJiraRequest(File file) {
		return JiraRequest.builder()
				.summary(format(SUMMARY))
				.description(format(DESCRIPTION))
				.reporterName(SKANMOTOVRIG_JIRA_BRUKER_NAVN)
				.labels(List.of("skanmotovrig_avvik"))
				.file(file)
				.build();
	}
}
