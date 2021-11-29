package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.helse.AbstractIt;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.EnableRetry;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@EnableRetry
class OpprettJournalpostConsumerTest extends AbstractIt {

	@Autowired
	OpprettJournalpostConsumer opprettJournalpostConsumer;

	@Autowired
	private SkanmotovrigProperties properties;


	@Test
	public void shouldGetJournalpostWhenResponseIs () {
		StubOpprettJournalpostResponseConflictWithValidResponse();
		OpprettJournalpostRequest request = OpprettJournalpostRequest.builder().build();

		OpprettJournalpostResponse response = opprettJournalpostConsumer.opprettJournalpost("token", request);
		assertEquals("567010363", response.getJournalpostId());
	}

	@Test
	public void shouldNotGetJournalpostWhenConflictDoesNotCorrectHaveBody() {
		StubOpprettJournalpostResponseConflictWithInvalidResponse();

		assertThrows(
				SkanmotovrigFunctionalException.class,
				() -> opprettJournalpostConsumer.opprettJournalpost("token", null)
		);
		verify(exactly(5), postRequestedFor(urlMatching("/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false")));
	}

}