package no.nav.skanmotovrig.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.itest.config.TestConfig;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostConsumer;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lesoglagre.LesFraFilomraadeOgOpprettJournalpost;
import no.nav.skanmotovrig.filomraade.FilomraadeConsumer;
import no.nav.skanmotovrig.filomraade.FilomraadeService;
import no.nav.skanmotovrig.sftp.Sftp;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgOpprettJournalpostIT {

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String STSUrl = "/rest/v1/sts/token";
    private static final String VALID_PUBLIC_KEY_PATH = "src/test/resources/sftp/itest_valid.pub";

    LesFraFilomraadeOgOpprettJournalpost lesFraFilomraadeOgOpprettJournalpost;
    FilomraadeService filomraadeService;
    OpprettJournalpostService opprettJournalpostService;

    private int PORT = 2222;
    private SshServer sshd = SshServer.setUpDefaultServer();
    private Sftp sftp;

    @Autowired
    SkanmotovrigProperties skanmotovrigeProperties;

    @BeforeAll
    void startSftpServer() throws IOException {
        sshd.setPort(PORT);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("src/test/resources/sftp/itest.ser")));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(Paths.get(VALID_PUBLIC_KEY_PATH)));
        sshd.start();
    }

    @AfterAll
    void shutdownSftpServer() throws IOException {
        sshd.stop();
        sshd.close();
    }

    @BeforeEach
    void setUpServices() {
        sftp = new Sftp(skanmotovrigeProperties);
        filomraadeService = new FilomraadeService(new FilomraadeConsumer(sftp, skanmotovrigeProperties));
        opprettJournalpostService = new OpprettJournalpostService(new OpprettJournalpostConsumer(new RestTemplateBuilder(), skanmotovrigeProperties));
        lesFraFilomraadeOgOpprettJournalpost = new LesFraFilomraadeOgOpprettJournalpost(filomraadeService, opprettJournalpostService);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpHappyStubs() {
        stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
        stubFor(post(urlMatching(STSUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        )))
        );
    }

    private void setUpBadStubs() {
        stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
        stubFor(post(urlMatching(STSUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                                "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                        )))
        );
    }

    @Test
    public void shouldLesOgLagreHappy() {
        setUpHappyStubs();
        assertDoesNotThrow(() -> lesFraFilomraadeOgOpprettJournalpost.lesOgLagre());
        verify(exactly(10), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldMoveFilesWhenBadRequest() {
        setUpBadStubs();
        lesFraFilomraadeOgOpprettJournalpost.lesOgLagre();
        assertTrue("foo".equals("bar"));
    }
}