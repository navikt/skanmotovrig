package no.nav.skanmotovrig.helse;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = HelseTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class PostboksHelseRouteIT {
    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";
    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String STS_URL = "/rest/v1/sts/token";

    @Inject
    private Path sshdPath;

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        preparePath(inngaaende);
        preparePath(processed);
        preparePath(feilmappe);
    }

    private void preparePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        } else {
            FileUtils.cleanDirectory(path.toFile());
        }
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void shouldBehandlePostboksHelseZip() throws IOException {
        // BHELSE-20200529-1.zip
        // OK   - BHELSE-20200529-1-1 xml, pdf
        // OK   - BHELSE-20200529-1-2 xml, pdf, ocr
        // FEIL - BHELSE-20200529-1-3 xml, pdf, ocr (valideringsfeil xml)
        // FEIL - BHELSE-20200529-1-4 xml, ocr (mangler pdf)
        // FEIL - BHELSE-20200529-1-5 pdf, ocr (mangler xml)

        copyFileFromClasspathToInngaaende("BHELSE-20200529-1.zip");
        setUpHappyStubs();

        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE)).collect(Collectors.toList())).hasSize(3));
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertThat(feilmappeContents).containsExactlyInAnyOrder(
                "BHELSE-20200529-1-3-funksjonelt.zip",
                "BHELSE-20200529-1-4-funksjonelt.zip",
                "BHELSE-20200529-1-5-funksjonelt.zip");
        verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldBehandlePostboksHelseZipWithMultipleDotsInFilenames() throws IOException {
        // BHELSE.20200529-2.zip
        // OK   - BHELSE.20200529-2-1 xml, pdf
        // OK   - BHELSE.20200529-2-2 xml, pdf, ocr
        // FEIL - BHELSE.20200529-2-3 xml, pdf, ocr (valideringsfeil xml)
        // FEIL - BHELSE.20200529-2-4 xml, ocr (mangler pdf)
        // FEIL - BHELSE.20200529-2-5 pdf, ocr (mangler xml)

        copyFileFromClasspathToInngaaende("BHELSE.20200529-2.zip");
        setUpHappyStubs();

        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE)).collect(Collectors.toList())).hasSize(3));
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertThat(feilmappeContents).containsExactlyInAnyOrder(
                "BHELSE.20200529-2-3-funksjonelt.zip",
                "BHELSE.20200529-2-4-funksjonelt.zip",
                "BHELSE.20200529-2-5-funksjonelt.zip");
        verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
        Files.copy(new ClassPathResource("__files/" + zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
    }

    private void setUpHappyStubs() {
        stubFor(post(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("journalpostapi/success.json")));
        stubFor(post(urlMatching(STS_URL))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("sts/token.json"))
        );
    }
}