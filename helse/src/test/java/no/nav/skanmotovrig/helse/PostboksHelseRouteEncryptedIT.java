package no.nav.skanmotovrig.helse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PostboksHelseRouteEncryptedIT extends AbstractIt {
	private static final String INNGAAENDE = "inngaaende";
	private static final String FEILMAPPE = "feilmappe";
	private static final String BATCHNAME_1 = "BHELSE-20200529-3";
	private static final String BATCHNAME_2 = "BHELSE.20200529-3";
	private static final String ZIP_FILENAME_NO_EXTENSION_BAD_ENCRYPTION = "BHELSE-20200529-BAD-ENCRYPTION-3";
	private static final String ZIP_FILE_NAME_NOT_ENCRYPTED_ENC = "BHELSE-XML-ORDERED-UKRYPTERED-4";

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

	@Test
	@Disabled
	public void shouldBehandlePostboksHelseEncrptedZip() throws IOException {
		// BHELSE-20200529-3.enc.zip
		// OK   - BHELSE-20200529-3-1 xml, pdf
		// OK   - BHELSE-20200529-3-2 xml, pdf, ocr
		// FEIL - BHELSE-20200529-3-3 xml, pdf, ocr (valideringsfeil xml)
		// FEIL - BHELSE-20200529-3-4 xml, ocr (mangler pdf)
		// FEIL - BHELSE-20200529-3-5 pdf, ocr (mangler xml)

		copyFileFromClasspathToInngaaende("BHELSE-20200529-3.enc.zip");
		setUpHappyStubs();

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(BATCHNAME_1))
						.collect(Collectors.toList())).hasSize(3);
			} catch (NoSuchFileException e) {
				fail();
			}
		});


		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(BATCHNAME_1))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"BHELSE-20200529-3-3.zip",
				"BHELSE-20200529-3-4.zip",
				"BHELSE-20200529-3-5.zip");
		verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
	}

	@Test
	public void shouldBehandlePostboksHelseEncryptedZipWithMultipleDotsInFilenames() throws IOException {
		// BHELSE.20200529-3.enc.zip
		// OK   - BHELSE.20200529-3-1 xml, pdf
		// OK   - BHELSE.20200529-3-2 xml, pdf, ocr
		// FEIL - BHELSE.20200529-3-3 xml, pdf, ocr (valideringsfeil xml)
		// FEIL - BHELSE.20200529-3-4 xml, ocr (mangler pdf)
		// FEIL - BHELSE.20200529-3-5 pdf, ocr (mangler xml)

		copyFileFromClasspathToInngaaende("BHELSE.20200529-3.enc.zip");
		setUpHappyStubs();

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				assertThat(Files.list(sshdPath.resolve(FEILMAPPE).resolve(BATCHNAME_2))
						.collect(Collectors.toList())).hasSize(3);
			} catch (NoSuchFileException e) {
				fail();
			}
		});

		final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE).resolve(BATCHNAME_2))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());
		assertThat(feilmappeContents).containsExactlyInAnyOrder(
				"BHELSE.20200529-3-3.zip",
				"BHELSE.20200529-3-4.zip",
				"BHELSE.20200529-3-5.zip");
		verify(exactly(2), postRequestedFor(urlMatching(URL_DOKARKIV_JOURNALPOST_GEN)));
	}


	@Test
	public void shouldMoveZipToFeilomraadeWhenNotEncryptedEncFile() throws IOException {
		//ZipException: En .enc-file kom inn men filene er ukrypterte
		//should be sent to feilmappe

		copyFileFromClasspathToInngaaende(ZIP_FILE_NAME_NOT_ENCRYPTED_ENC + ".enc.zip");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
						.map(p -> wiremock.org.apache.commons.io.FilenameUtils.getName(p.toAbsolutePath().toString()))
						.collect(Collectors.toList());
				assertTrue(feilmappeContents.contains(ZIP_FILE_NAME_NOT_ENCRYPTED_ENC + ".enc.zip"));
			} catch (NoSuchFileException e) {
				fail();
			}
		});
	}

	@Test
	public void shouldMoveZipToFeilomraadeWhenBadEncryption() throws IOException {
		//ZipException: Filene er ikke kryptert med AES men en annen krypteringsmetode
		//should be sent to feilmappe

		copyFileFromClasspathToInngaaende(ZIP_FILENAME_NO_EXTENSION_BAD_ENCRYPTION + ".enc.zip");

		await().atMost(15, SECONDS).untilAsserted(() -> {
			try {
				final List<String> feilmappeContents = Files.list(sshdPath.resolve(FEILMAPPE))
						.map(p -> wiremock.org.apache.commons.io.FilenameUtils.getName(p.toAbsolutePath().toString()))
						.collect(Collectors.toList());
				assertTrue(feilmappeContents.contains(ZIP_FILENAME_NO_EXTENSION_BAD_ENCRYPTION + ".enc.zip"));
			} catch (NoSuchFileException e) {
				fail();
			}
		});
	}

	private void copyFileFromClasspathToInngaaende(final String zipfilename) throws IOException {
		Files.copy(new ClassPathResource(zipfilename).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(zipfilename));
	}
}