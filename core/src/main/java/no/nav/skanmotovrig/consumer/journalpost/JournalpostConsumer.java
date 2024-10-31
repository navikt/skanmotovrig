package no.nav.skanmotovrig.consumer.journalpost;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.consumer.journalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.consumer.journalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.consumer.sts.STSConsumer;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.utils.NavHeaders;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class JournalpostConsumer {

	private final RestTemplate restTemplate;
	private final String dokarkivJournalpostUrl;
	private final ObjectMapper mapper;
	private final STSConsumer stsConsumer;

	public JournalpostConsumer(
			RestTemplateBuilder restTemplateBuilder,
			SkanmotovrigProperties skanmotovrigProperties,
			ObjectMapper mapper,
			STSConsumer stsConsumer
	) {
		this.dokarkivJournalpostUrl = skanmotovrigProperties.getDokarkivjournalposturl();
		this.mapper = mapper;
		this.stsConsumer = stsConsumer;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(150))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest opprettJournalpostRequest) {
		try {
			HttpHeaders headers = createHeaders();
			HttpEntity<OpprettJournalpostRequest> requestEntity = new HttpEntity<>(opprettJournalpostRequest, headers);
			return restTemplate.exchange(dokarkivJournalpostUrl + "/journalpost?foersoekFerdigstill=false", POST, requestEntity, OpprettJournalpostResponse.class).getBody();
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

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(stsConsumer.getSTSToken().getAccess_token());
		headers.addAll(NavHeaders.createNavCustomHeaders());
		return headers;
	}
}
