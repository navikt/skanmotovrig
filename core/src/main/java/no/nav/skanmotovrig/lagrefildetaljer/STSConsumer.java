package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSResponse;
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

import java.util.Collections;

import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

@Component
public class STSConsumer {
    private final String urlEncodedBody = "grant_type=client_credentials&scope=openid";

    private final RestTemplate restTemplate;
    private final String stsUrl;

    public STSConsumer(RestTemplateBuilder restTemplateBuilder,
                                      SkanmotovrigProperties skanmotovrigProperties) {
        this.stsUrl = skanmotovrigProperties.getStsurl();
        this.restTemplate = restTemplateBuilder
                .basicAuthentication(skanmotovrigProperties.getServiceuser().getUsername(),
                        skanmotovrigProperties.getServiceuser().getPassword())
                .build();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "getSTSToken"}, percentiles = {0.5, 0.95}, histogram = true, createErrorMetric = true)
    public STSResponse getSTSToken() {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(urlEncodedBody, headers);

            return restTemplate.exchange(stsUrl, HttpMethod.POST, requestEntity, STSResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            throw new SkanmotovrigFunctionalException(String.format("getSTSToken feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        } catch (HttpServerErrorException e) {
            throw new SkanmotovrigTechnicalException(String.format("getSTSToken feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }


    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.addAll(NavHeaders.createNavCustomHeaders());
        return headers;
    }
}
