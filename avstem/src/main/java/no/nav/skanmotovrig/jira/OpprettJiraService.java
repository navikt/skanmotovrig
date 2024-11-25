package no.nav.skanmotovrig.jira;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.JiraResponse;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class OpprettJiraService {

	private static final String DESCRIPTION = "Se vedlegg for en oversikt over manglende avstemmingsreferanser for skannede dokumenter fra skanmotøvrig \n";
	public static final String SUMMARY = "Skanmotøvrig: Manglende avstemmingsreferanser for skannede dokumenter";
	private static final String SKANMOTOVRIG_JIRA_BRUKER_NAVN = "srvjiradokdistavstemming";
	public static final String ANTALL_FILER_AVSTEMT = "Antall filer avstemt";
	public static final String ANTALL_FILER_FEILET = "Antall filer feilet";
	private final JiraService jiraService;

	public OpprettJiraService(JiraService jiraService) {
		this.jiraService = jiraService;
	}


	@Handler
	public JiraResponse opprettAvstemJiraOppgave(byte[] csvByte, Exchange exchange) {
		Integer antallAvstemt = exchange.getProperty(ANTALL_FILER_AVSTEMT, Integer.class);
		Integer antallFeilet = exchange.getProperty(ANTALL_FILER_FEILET, Integer.class);

		try {
			if (csvByte == null) {
				log.warn("fant ikke feilende avstemmingsfil og kan ikke opprette jira oppgave");
				return null;
			}
			
			File file = createFile(csvByte);
			JiraRequest jiraRequest = mapJiraRequest(file, new AvstemtFiler(antallAvstemt, antallFeilet));
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

	private JiraRequest mapJiraRequest(File file, AvstemtFiler avstemtFiler) {
		return JiraRequest.builder()
				.summary(SUMMARY)
				.description(prettifySummary(avstemtFiler))
				.reporterName(SKANMOTOVRIG_JIRA_BRUKER_NAVN)
				.labels(List.of("skanmotovrig_avvik"))
				.file(file)
				.build();
	}

	private String prettifySummary(AvstemtFiler avstemtFiler) {
		StringBuilder builder = new StringBuilder();
		return builder.append(DESCRIPTION)
				.append("\nAntall filer avstemt: ").append(avstemtFiler.antallAvstemt())
				.append("\nAntall filer funnet: ").append(avstemtFiler.antallAvstemt() - avstemtFiler.antallFeilet())
				.append("\nAntall filer feilet: ").append(avstemtFiler.antallFeilet()).toString();

	}
}
