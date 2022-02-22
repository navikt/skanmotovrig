package no.nav.skanmotovrig.ovrig;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = OvrigTestConfig.class,
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class AbstractIt {

	public static final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
	private static final String STS_URL = "/rest/v1/sts/token";

	public void setUpMocks() {
		stubFor(post(urlMatching(STS_URL))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withHeader("Connection", "close")
						.withBodyFile("sts/token.json"))
		);

		stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withHeader("Connection", "close")
						.withBodyFile("journalpostapi/success.json")));
	}
}
