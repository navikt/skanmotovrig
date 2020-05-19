package no.nav.skanmotovrig.lagrefildetaljer;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.constants.MDCConstants;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFinnesIkkeFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigTillaterIkkeTilknyttingFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSResponse;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

@Slf4j
public class STSInterceptor implements ClientHttpRequestInterceptor {

    private final RestTemplate restTemplate;
    private final String stsUrl;
    private final String dokarkivJournalpostUrl;
    private final String urlEncodedBody = "grant_type=client_credentials&scope=openid";

//    private final String username;
//    private final String password;

    STSInterceptor(SkanmotovrigProperties skanmotovrigProperties){
        this.stsUrl = skanmotovrigProperties.getStsurl();
        this.dokarkivJournalpostUrl = skanmotovrigProperties.getDokarkivjournalposturl();
        this.restTemplate = new RestTemplateBuilder()
                .setReadTimeout(Duration.ofSeconds(150))
                .setConnectTimeout(Duration.ofSeconds(5))
                .basicAuthentication(skanmotovrigProperties.getServiceuser().getUsername(),
                        skanmotovrigProperties.getServiceuser().getPassword())
                .build();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        try{
            log.info("Kaller STS");
            HttpHeaders stsHeaders = createHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(urlEncodedBody, stsHeaders);
            STSResponse stsResponse = restTemplate.exchange(stsUrl, HttpMethod.POST, requestEntity, STSResponse.class)
                    .getBody();

            httpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            httpRequest.getHeaders().setBearerAuth(stsResponse.getAccess_token());
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        } catch (HttpClientErrorException e) {
            if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
                throw new SkanmotovrigFinnesIkkeFunctionalException(String.format("STSInterceptor feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            } else if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                throw new SkanmotovrigTillaterIkkeTilknyttingFunctionalException(String.format("STSInterceptor feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            } else {
                throw new SkanmotovrigFunctionalException(String.format("STSInterceptor feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getMessage()), e);
            }
        } catch (HttpServerErrorException e) {
            throw new SkanmotovrigTechnicalException(String.format("STSInterceptor feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (MDC.get(MDCConstants.MDC_NAV_CALL_ID) != null) {
            headers.add(MDCConstants.MDC_NAV_CALL_ID, MDC.get(MDCConstants.MDC_NAV_CALL_ID));
        }
        if (MDC.get(MDCConstants.MDC_NAV_CONSUMER_ID) != null) {
            headers.add(MDCConstants.MDC_NAV_CONSUMER_ID, MDC.get(MDCConstants.MDC_NAV_CONSUMER_ID));
        }
        return headers;
    }
}
