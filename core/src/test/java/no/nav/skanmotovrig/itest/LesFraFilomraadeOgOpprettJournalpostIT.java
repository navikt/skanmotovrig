package no.nav.skanmotovrig.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.skanmotovrig.LesFraFilomraadeOgOpprettJournalpost;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.filomraade.FilomraadeConsumer;
import no.nav.skanmotovrig.filomraade.FilomraadeService;
import no.nav.skanmotovrig.itest.config.TestConfig;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostConsumer;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.STSConsumer;
import no.nav.skanmotovrig.metrics.DokCounter;
import no.nav.skanmotovrig.sftp.Sftp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wiremock.org.apache.commons.io.FileUtils;
import wiremock.org.apache.commons.io.FilenameUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("itest")
public class LesFraFilomraadeOgOpprettJournalpostIT {

    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";

    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String STSUrl = "/rest/v1/sts/token";

    private final String ZIP_FILE_NAME_NO_EXTENSION = "01.07.2020_R123456789_1_1000";

    private LesFraFilomraadeOgOpprettJournalpost lesFraFilomraadeOgOpprettJournalpost;
    private OpprettJournalpostService opprettJournalpostService;
    private DokCounter dokCounter;
    private FilomraadeService filomraadeService;

    private Sftp sftp;

    @Inject
    private Path sshdPath;

    @Autowired
    SkanmotovrigProperties skanmotovrigeProperties;


    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        preparePath(inngaaende);
        preparePath(processed);
        preparePath(feilmappe);
        setUpServices();
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void shouldLesOgLagreZip() throws IOException {
        // 01.07.2020_R123456789_1_1000.zip
        // OK   - 01.07.2020_R123456789_0001
        // OK   - 01.07.2020_R123456789_0002 (mangler filnavn og bruker)
        // FEIL - 01.07.2020_R123456789_0003 (valideringsfeil, mangler brukertype)
        // FEIL - 01.07.2020_R123456789_0004 (feiler mot dokarkiv)
        // FEIL - 01.07.2020_R123456789_0005 (mangler pdf)
        // FEIL - 01.07.2020_R123456789_0006 (mangler xml)
        setUpHappyStubs();
        setUpBadStubs();
        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip");

        assertDoesNotThrow(() ->
                assertEquals("#ZIPFILES=1, #FILEPAIRS=6, #FAILING=4",
                        lesFraFilomraadeOgOpprettJournalpost.lesOgLagre()
                )
        );

        Path pat = sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION);
        assertEquals(6, Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION)).count());
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertTrue(feilmappeContents.containsAll(List.of(
                "01.07.2020_R123456789_0003.pdf",
                "01.07.2020_R123456789_0003.xml",
                "01.07.2020_R123456789_0004.pdf",
                "01.07.2020_R123456789_0004.xml",
                "01.07.2020_R123456789_0005.xml",
                "01.07.2020_R123456789_0006.pdf"
        )));
        verify(exactly(3), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    void setUpServices() {
        sftp = new Sftp(skanmotovrigeProperties);
        filomraadeService = Mockito.spy(new FilomraadeService(new FilomraadeConsumer(sftp, skanmotovrigeProperties)));
        opprettJournalpostService = new OpprettJournalpostService(
                new OpprettJournalpostConsumer(new RestTemplateBuilder(), skanmotovrigeProperties),
                new STSConsumer(new RestTemplateBuilder(), skanmotovrigeProperties)
        );
        dokCounter = new DokCounter(new SimpleMeterRegistry());
        lesFraFilomraadeOgOpprettJournalpost = new LesFraFilomraadeOgOpprettJournalpost(filomraadeService, opprettJournalpostService, dokCounter);
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
                .withRequestBody(equalToJson("{ \"tema\": \"INV\" }", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("{}")));
    }

    private void preparePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        } else {
            FileUtils.cleanDirectory(path.toFile());
        }
    }

    private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
        Files.copy(new ClassPathResource("__files/" + zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
    }
}