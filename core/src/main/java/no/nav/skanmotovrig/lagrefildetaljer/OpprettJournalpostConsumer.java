package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFinnesIkkeFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.metrics.Metrics;
import no.nav.skanmotovrig.constants.MDCConstants;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

@Component
public class OpprettJournalpostConsumer {

    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "mottaDokumentUtgaaendeSkanning";
    private final String MOTTA_OVRIG_SKANNING_TJENESTE = "mottaOvrigSkanning";

    private final RestTemplate restTemplate;
    private final String dokarkivJournalpostUrl;

    public OpprettJournalpostConsumer(RestTemplateBuilder restTemplateBuilder,
                                      SkanmotovrigProperties skanmotovrigProperties) {
        this.dokarkivJournalpostUrl = skanmotovrigProperties.getDokarkivjournalposturl();
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(150))
                .setConnectTimeout(Duration.ofSeconds(5))
                .basicAuthentication(skanmotovrigProperties.getServiceuser().getUsername(),
                        skanmotovrigProperties.getServiceuser().getPassword())
                .build();
        this.restTemplate.setInterceptors(Collections.singletonList(new STSInterceptor(skanmotovrigProperties)));
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "lagreFilDetaljer"}, percentiles = {0.5, 0.95}, histogram = true)
    public OpprettJournalpostResponse lagreFilDetaljer(OpprettJournalpostRequest opprettJournalpostRequest) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<OpprettJournalpostRequest> requestEntity = new HttpEntity<>(opprettJournalpostRequest, headers);

            URI uri = new URI(dokarkivJournalpostUrl);
            return restTemplate.exchange(uri, HttpMethod.POST, requestEntity, OpprettJournalpostResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new SkanmotovrigFunctionalException(String.format("opprettJournalpost feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new SkanmotovrigTechnicalException(String.format("opprettJournalpost feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (URISyntaxException e) {
            throw new SkanmotovrigTechnicalException(String.format("opprettJournalpost feilet teknisk. Feilmelding=%s",
                    e.getMessage()), e);
        }
    }


    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (MDC.get(MDCConstants.MDC_NAV_CALL_ID) != null) {
            headers.add(MDCConstants.MDC_NAV_CALL_ID, MDC.get(MDCConstants.MDC_NAV_CALL_ID));
        }
        if (MDC.get(MDCConstants.MDC_NAV_CONSUMER_ID) != null) {
            headers.add(MDCConstants.MDC_NAV_CONSUMER_ID, MDC.get(MDCConstants.MDC_NAV_CONSUMER_ID));
        }
        return headers;
    }
}
