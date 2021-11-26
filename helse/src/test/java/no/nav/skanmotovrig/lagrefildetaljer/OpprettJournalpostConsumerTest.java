package no.nav.skanmotovrig.lagrefildetaljer;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.helse.HelseTestConfig;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = HelseTestConfig.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.cloud.vault.token=123456"
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@EnableRetry
class OpprettJournalpostConsumerTest  {

	@Autowired
	OpprettJournalpostConsumer opprettJournalpostConsumer;

	@Autowired
	private SkanmotovrigProperties properties;

	@SneakyThrows
	@BeforeEach
	void setUp() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void shouldGetJournalpostWhenResponseIs () {
		this.StubOpprettJournalpostResponseConflictWithValidResponse();

		OpprettJournalpostResponse response = opprettJournalpostConsumer.opprettJournalpost("token", null);
		assertEquals("567010363", response.getJournalpostId());
	}

	@Test
	public void shouldNotGetJournalpostWhenConflictDoesNotCorrectHaveBody() {
		this.StubOpprettJournalpostResponseConflictWithInvalidResponse();

		assertThrows(
				SkanmotovrigFunctionalException.class,
				() -> opprettJournalpostConsumer.opprettJournalpost("token", null)
		);
		verify(exactly(5), postRequestedFor(urlMatching("/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false")));
	}

	public void StubOpprettJournalpostResponseConflictWithValidResponse() {
		stubFor(post("/rest/journalpostapi/v1/journalpost?foersoekFerdigstill=false").willReturn(aResponse()
				.withStatus(CONFLICT.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/journalpostapi/allerede_opprett_journalpost_response_HAPPY.json"))));
	}

	protected void StubOpprettJournalpostResponseConflictWithInvalidResponse() {
		stubFor(post("/rest/journalpostapi/v1/journalpost?foersoekFerdigstill=false").willReturn(aResponse()
				.withStatus(CONFLICT.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	@SneakyThrows
	private static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);
	}
}