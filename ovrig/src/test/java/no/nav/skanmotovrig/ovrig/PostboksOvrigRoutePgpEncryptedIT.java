package no.nav.skanmotovrig.ovrig;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class PostboksOvrigRoutePgpEncryptedIT extends AbstractIt {

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		super.setUpMocks();
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

	@Test
	public void shouldBehandlePostboksOvrigZip() throws IOException {
		// OVRIG-20200529-4.zip.pgp
		// OK   - OVRIG-20200529-4-1 alle felt
		// OK   - OVRIG-20200529-4-2 kun påkrevde felt
		// OK   - OVRIG-20200529-4-3 tomme valgfri felt
		// FEIL - OVRIG-20200529-4-4 xml (mangler pdf)
		// FEIL - OVRIG-20200529-4-5 pdf (mangler xml)
		// FEIL - OVRIG-20200529-4-6 malformet xml

		final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG-20200529-4";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
						.collect(Collectors.toList())).hasSize(3);

				verify(exactly(3), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
				verify(exactly(2), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
						.withRequestBody(containing("no.nav.skanmotovrig.exceptions.functional.ForsendelseNotCompleteException")));
				verify(exactly(1), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
						.withRequestBody(containing("no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException")));
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"OVRIG-20200529-4-4.zip",
				"OVRIG-20200529-4-5.zip",
				"OVRIG-20200529-4-6.zip");
	}

	@Test
	public void shouldBehandlePostboksOvrigZipWithMultipleDotsInFilenames() throws IOException {
		// OVRIG.20200529-4.zip.pgp
		// OK   - OVRIG.20200529-4-1 alle felt
		// OK   - OVRIG.20200529-4-2 kun påkrevde felt
		// OK   - OVRIG.20200529-4-3 tomme valgfri felt
		// FEIL - OVRIG.20200529-4-4 xml (mangler pdf)
		// FEIL - OVRIG.20200529-4-5 pdf (mangler xml)
		// FEIL - OVRIG.20200529-4-6 malformet xml

		final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG.20200529-4";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
						.collect(Collectors.toList())).hasSize(3);

				verify(exactly(3), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
				verify(exactly(2), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
						.withRequestBody(containing("no.nav.skanmotovrig.exceptions.functional.ForsendelseNotCompleteException")));
				verify(exactly(1), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
						.withRequestBody(containing("no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException")));
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"OVRIG.20200529-4-4.zip",
				"OVRIG.20200529-4-5.zip",
				"OVRIG.20200529-4-6.zip");
	}

	@Test
	public void shouldBehandleZipXmlOrderedLastWithinCompletionTimeout() throws IOException {
		// OVRIG-XML-ORDERED-FIRST-1.zip.pgp
		// OK   - OVRIG-XML-ORDERED-FIRST-1-01 alle felt
		// OK   - OVRIG-XML-ORDERED-FIRST-1-02 kun påkrevde felt
		// OK   - OVRIG-XML-ORDERED-FIRST-1-03 tomme valgfri felt
		// FEIL - OVRIG-XML-ORDERED-FIRST-1-04 xml (mangler pdf)
		// FEIL - OVRIG-XML-ORDERED-FIRST-1-05 pdf (mangler xml)
		// FEIL - OVRIG-XML-ORDERED-FIRST-1-06 malformet xml
		// OK   - OVRIG-XML-ORDERED-FIRST-1-07 alle felt
		// ...
		// OK   - OVRIG-XML-ORDERED-FIRST-1-59 alle felt

		final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG-XML-ORDERED-FIRST-1";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
						.collect(Collectors.toList())).hasSize(3);

				verify(exactly(56), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
				verify(exactly(2), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
						.withRequestBody(containing("no.nav.skanmotovrig.exceptions.functional.ForsendelseNotCompleteException")));
				verify(exactly(1), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
						.withRequestBody(containing("no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException")));
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"OVRIG-XML-ORDERED-FIRST-1-04.zip",
				"OVRIG-XML-ORDERED-FIRST-1-05.zip",
				"OVRIG-XML-ORDERED-FIRST-1-06.zip");
	}


	@Test
	public void shouldFailWhenPrivateKeyDoesNotMatchPublicKey() throws IOException {
		// OVRIG-XML-ORDERED-FIRST-1_WRONG_PRIVATE_KEY.zip.pgp er kryptert med publicKeyElGamal (i stedet for publicKeyRSA)
		// Korresponderende RSA-private key vil da feile i forsøket på dekryptering
		final String ZIP_FILE_NAME_NO_EXTENSION = "OVRIG-XML-ORDERED-FIRST-1_WRONG_PRIVATE_KEY";
		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp");

		assertTrue(Files.exists(sshdPath.resolve(INNGAAENDE).resolve(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp")));

		await().atMost(15, SECONDS).untilAsserted(() -> {
			assertTrue(Files.exists(sshdPath.resolve(FEILMAPPE).resolve(ZIP_FILE_NAME_NO_EXTENSION + ".zip.pgp")));

			verify(exactly(1), postRequestedFor(urlPathEqualTo(SLACK_POST_MESSAGE_PATH))
					.withRequestBody(containing("org.bouncycastle.openpgp.PGPException")));
		});
	}

	private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
		Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
	}
}