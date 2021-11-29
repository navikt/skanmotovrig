package no.nav.skanmotovrig.ovrig;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = OvrigTestConfig.class,
		webEnvironment = RANDOM_PORT,
		properties = "spring.cloud.vault.token=123456"
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class AbstractIt {

	static final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
	private static final String STS_URL = "/rest/v1/sts/token";

	@BeforeEach
	void settUpMocks() {
		stubFor(post(urlMatching(STS_URL))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBodyFile("sts/token.json"))
		);
	}

	@BeforeEach
	void resetMocks() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	void setUpHappyStubs() {
		stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("journalpostapi/success.json")));
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
