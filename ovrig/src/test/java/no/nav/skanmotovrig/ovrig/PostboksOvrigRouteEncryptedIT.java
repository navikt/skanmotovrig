package no.nav.skanmotovrig.ovrig;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.nio.file.NoSuchFileException;
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
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = OvrigTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.cloud.vault.token=123456")
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class PostboksOvrigRouteEncryptedIT {
    public static final String INNGAAENDE = "inngaaende";
    public static final String FEILMAPPE = "feilmappe";
    private final String URL_DOKARKIV_JOURNALPOST_GEN = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String STS_URL = "/rest/v1/sts/token";

    @Inject
    private Path sshdPath;

    @BeforeEach
    void beforeEach() {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path processed = inngaaende.resolve("processed");
        final Path feilmappe = sshdPath.resolve(FEILMAPPE);
        try {
            preparePath(inngaaende);
            preparePath(processed);
            preparePath(feilmappe);
        } catch (Exception e) {
            // noop
        }
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
    public void shouldBehandlePostboksOvrigZip() throws IOException {
        // OVRIG-20200529-2.enc.zip
        // OK   - OVRIG-20200529-2-1 alle felt
        // OK   - OVRIG-20200529-2-2 kun påkrevde felt
        // OK   - OVRIG-20200529-2-3 tomme valgfri felt
        // FEIL - OVRIG-20200529-2-4 xml (mangler pdf)
        // FEIL - OVRIG-20200529-2-5 pdf (mangler xml)
        // FEIL - OVRIG-20200529-2-6 malformet xml

        final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG-20200529-2";

        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");
        setUpHappyStubs();

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                        .collect(Collectors.toList())).hasSize(3);
            } catch (NoSuchFileException e) {
                fail();
            }
        });
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertThat(feilmappeContents).containsExactlyInAnyOrder(
                "OVRIG-20200529-2-4.zip",
                "OVRIG-20200529-2-5.zip",
                "OVRIG-20200529-2-6.zip");
        verify(exactly(3), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldBehandlePostboksOvrigZipWithMultipleDotsInFilenames() throws IOException {
        // OVRIG.20200529-3.enc.enc.zip
        // OK   - OVRIG.20200529-3-1 alle felt
        // OK   - OVRIG.20200529-3-2 kun påkrevde felt
        // OK   - OVRIG.20200529-3-3 tomme valgfri felt
        // FEIL - OVRIG.20200529-3-4 xml (mangler pdf)
        // FEIL - OVRIG.20200529-3-5 pdf (mangler xml)
        // FEIL - OVRIG.20200529-3-6 malformet xml

        final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG.20200529-3";

        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");
        setUpHappyStubs();

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                        .collect(Collectors.toList())).hasSize(3);
            } catch (NoSuchFileException e) {
                fail();
            }
        });
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertThat(feilmappeContents).containsExactlyInAnyOrder(
                "OVRIG.20200529-3-4.zip",
                "OVRIG.20200529-3-5.zip",
                "OVRIG.20200529-3-6.zip");
        verify(exactly(3), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    @Test
    public void shouldBehandleZipXmlOrderedLastWithinCompletionTimeout() throws IOException {
        // OVRIG-XML-ORDERED-FIRST-2.enc.zip
        // OK   - OVRIG-XML-ORDERED-FIRST-2-01 alle felt
        // OK   - OVRIG-XML-ORDERED-FIRST-2-02 kun påkrevde felt
        // OK   - OVRIG-XML-ORDERED-FIRST-2-03 tomme valgfri felt
        // FEIL - OVRIG-XML-ORDERED-FIRST-2-04 xml (mangler pdf)
        // FEIL - OVRIG-XML-ORDERED-FIRST-2-05 pdf (mangler xml)
        // FEIL - OVRIG-XML-ORDERED-FIRST-2-06 malformet xml
        // OK   - OVRIG-XML-ORDERED-FIRST-2-07 alle felt
        // ...
        // OK   - OVRIG-XML-ORDERED-FIRST-1-59 alle felt

        final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG-XML-ORDERED-FIRST-2";

        copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".enc.zip");
        setUpHappyStubs();

        await().atMost(15, SECONDS).untilAsserted(() -> {
            try {
                assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                        .collect(Collectors.toList())).hasSize(3);
            } catch (NoSuchFileException e) {
                fail();
            }
        });
        final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
                .map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
                .collect(Collectors.toList());
        assertThat(feilmappeContents).containsExactlyInAnyOrder(
                "OVRIG-XML-ORDERED-FIRST-2-04.zip",
                "OVRIG-XML-ORDERED-FIRST-2-05.zip",
                "OVRIG-XML-ORDERED-FIRST-2-06.zip");
        verify(exactly(56), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
    }

    private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
        Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
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