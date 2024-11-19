package no.nav.skanmotovrig.consumer.journalpost;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.consumer.journalpost.data.AvstemmingReferanser;
import no.nav.skanmotovrig.consumer.journalpost.data.FeilendeAvstemmingReferanser;
import no.nav.skanmotovrig.consumer.journalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.consumer.journalpost.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import no.nav.skanmotovrig.utils.NavHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.skanmotovrig.azure.OAuthEnabledWebClientConfig.CLIENT_REGISTRATION_DOKARKIV;
import static no.nav.skanmotovrig.utils.RetryConstants.MAX_RETRIES;
import static no.nav.skanmotovrig.utils.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.skanmotovrig.utils.RetryConstants.RETRY_DELAY;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static reactor.core.publisher.Mono.just;

@Slf4j
@Component
public class JournalpostConsumer {

	private final WebClient webClient;

	public JournalpostConsumer(
			WebClient webClient,
			SkanmotovrigProperties skanmotovrigProperties
	) {
		this.webClient = webClient.mutate()
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.baseUrl(skanmotovrigProperties.getEndpoints().getDokarkiv().getUrl())
				.build();
	}

	@Retryable(retryFor = SkanmotovrigTechnicalException.class,
			maxAttempts = MAX_RETRIES,
			backoff = @Backoff(delay = RETRY_DELAY, multiplier = MULTIPLIER_SHORT))
	public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest opprettJournalpostRequest) {
		return webClient.post()
				.uri("/journalpost?foersoekFerdigstill=false")
				.headers(NavHeaders::createNavCustomHeaders)
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.bodyValue(opprettJournalpostRequest)
				.exchangeToMono(response -> {
					if (response.statusCode().isError()) {
						if (response.statusCode().isSameCodeAs(CONFLICT)) {
							Mono<OpprettJournalpostResponse> journalpostResponse = response.bodyToMono(OpprettJournalpostResponse.class);
							log.info("Det eksisterer allerede en journalpost i dokarkiv med eksternReferanseId={} og kan ikke opprette ny journalpost.",
									opprettJournalpostRequest.getEksternReferanseId());
							return journalpostResponse;
						}
						return response.createError();
					}
					return response.bodyToMono(OpprettJournalpostResponse.class);
				})
				.doOnError(handleError("opprettJournalpost"))
				.block();
	}

	@Retryable(retryFor = SkanmotovrigTechnicalException.class, backoff = @Backoff(delay = RETRY_DELAY))
	public FeilendeAvstemmingReferanser avstemReferanser(AvstemmingReferanser avstemmingReferanser) {

		return webClient.post()
				.uri("/avstemReferanser")
				.headers(NavHeaders::createNavCustomHeaders)
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.body(just(avstemmingReferanser), AvstemmingReferanser.class)
				.retrieve()
				.bodyToMono(FeilendeAvstemmingReferanser.class)
				.doOnError(handleError("avstemReferanser"))
				.block();
	}

	private Consumer<Throwable> handleError(String melding) {
		return error -> {
			if (error instanceof WebClientResponseException webException && webException.getStatusCode().is4xxClientError()) {
				throw new SkanmotovrigFunctionalException(format("%s feilet funksjonelt med statusKode=%s. Feilmelding=%s", melding,
						webException.getStatusCode(), webException.getMessage()), error);

			}
			throw new SkanmotovrigTechnicalException(format("%s feilet teknisk med Feilmelding=%s", melding, error.getMessage()), error);
		};
	}
}
