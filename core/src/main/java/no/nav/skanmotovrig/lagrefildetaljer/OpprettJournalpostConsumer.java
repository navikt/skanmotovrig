package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.metrics.Metrics;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

@Component
public class OpprettJournalpostConsumer {

    private final RestTemplate restTemplate;
    private final String dokarkivJournalpostUrl;

    public OpprettJournalpostConsumer(RestTemplateBuilder restTemplateBuilder,
                                      SkanmotovrigProperties skanmotovrigProperties) {
        this.dokarkivJournalpostUrl = skanmotovrigProperties.getDokarkivjournalposturl();
        this.restTemplate = restTemplateBuilder
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "lagreFilDetaljer"}, percentiles = {0.5, 0.95}, histogram = true)
    public OpprettJournalpostResponse opprettJournalpost(String token, OpprettJournalpostRequest opprettJournalpostRequest) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<OpprettJournalpostRequest> requestEntity = new HttpEntity<>(opprettJournalpostRequest, headers);
            return restTemplate.exchange(dokarkivJournalpostUrl, HttpMethod.POST, requestEntity, OpprettJournalpostResponse.class).getBody();
        } catch (HttpClientErrorException e) {
            throw new SkanmotovrigFunctionalException(String.format("opprettJournalpost feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new SkanmotovrigTechnicalException(String.format("opprettJournalpost feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }


    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.addAll(NavHeaders.createNavCustomHeaders());
        return headers;
    }
}
