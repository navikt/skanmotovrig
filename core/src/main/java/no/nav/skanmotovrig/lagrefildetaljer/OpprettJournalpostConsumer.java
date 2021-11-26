package no.nav.skanmotovrig.lagrefildetaljer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.metrics.Metrics;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static no.nav.skanmotovrig.lagrefildetaljer.RetryConstants.RETRY_DELAY;
import static no.nav.skanmotovrig.lagrefildetaljer.RetryConstants.MAX_RETRIES;
import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class OpprettJournalpostConsumer {

	private final RestTemplate restTemplate;
	private final String dokarkivJournalpostUrl;
	private final ObjectMapper mapper;

	public OpprettJournalpostConsumer(
			RestTemplateBuilder restTemplateBuilder,
			SkanmotovrigProperties skanmotovrigProperties,
			ObjectMapper mapper
	) {
		this.dokarkivJournalpostUrl = skanmotovrigProperties.getDokarkivjournalposturl();
		this.mapper = mapper;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(150))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Retryable(maxAttempts = MAX_RETRIES, backoff = @Backoff(delay = RETRY_DELAY))
	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "opprettJournalpost"}, percentiles = {0.5, 0.95}, histogram = true)
	public OpprettJournalpostResponse opprettJournalpost(String token, OpprettJournalpostRequest opprettJournalpostRequest) {
		try {
			HttpHeaders headers = createHeaders(token);
			HttpEntity<OpprettJournalpostRequest> requestEntity = new HttpEntity<>(opprettJournalpostRequest, headers);
			return restTemplate.exchange(dokarkivJournalpostUrl, POST, requestEntity, OpprettJournalpostResponse.class).getBody();
		} catch (HttpClientErrorException e) {
			if (CONFLICT == e.getStatusCode()) {
				try {
					OpprettJournalpostResponse journalpost = mapper.readValue(e.getResponseBodyAsString(), OpprettJournalpostResponse.class);
					log.info("Det eksisterer allerede en journalpost i dokarkiv med fil={}. Denne har journalpostId={}. Oppretter ikke ny journalpost.",
							opprettJournalpostRequest.getEksternReferanseId(),
							journalpost.getJournalpostId());
					return journalpost;
				} catch (JsonProcessingException jsonProcessingException) {
					throw new SkanmotovrigFunctionalException("Ikke mulig å konvertere respons ifra dokarkiv.", e);
				}
			}
			throw new SkanmotovrigFunctionalException(String.format("opprettJournalpost feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new SkanmotovrigTechnicalException(String.format("opprettJournalpost feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(token);
		headers.addAll(NavHeaders.createNavCustomHeaders());
		return headers;
	}
}
