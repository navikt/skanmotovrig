package no.nav.skanmotovrig.ovrig;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = OvrigTestConfig.class,
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public abstract class AbstractIt {

	public static final String INNGAAENDE = "inngaaende";
	public static final String FEILMAPPE = "feilmappe";
	public static final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
	static final String SLACK_POST_MESSAGE_PATH = "/slack/api/chat.postMessage";
	private static final String SLACK_AUTH_PATH = "/slack/api/auth.test";

	public void setUpMocks() {
		stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withHeader("Connection", "close")
						.withBodyFile("journalpostapi/success.json")));

		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));

		stubFor(post(urlPathEqualTo(SLACK_AUTH_PATH))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withBodyFile("slack/auth_response.json")));

		stubFor(post(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withBodyFile("slack/message_response.json")));
	}
}
