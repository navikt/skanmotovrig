package no.nav.skanmotovrig.itest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

public class AvstemJobbRouteIT extends AbstractIT {

	public static final String AVSTEM = "avstemmappe";
	public static final String PROCESSED = "processed";

	@Autowired
	private Path sshdPath;

	@BeforeEach
	void beforeEach() {
		super.setUpMocks();
		final Path avstem = sshdPath.resolve(AVSTEM);
		final Path processed = avstem.resolve(PROCESSED);
		try {
			preparePath(avstem);
			preparePath(processed);
		} catch (Exception e) {
			// noop
		}
	}

	@Test
	public void shouldOpprettJiraOppgaveForFeilendeAvstemReferanser() throws IOException {
		stubJiraOpprettOppgave();
		final String AVSTEM_FIL = "04.11.2024_avstemmingsfil_1.txt";

		copyFileFromClasspathToAvstem(AVSTEM_FIL);

		assertThat(Files.exists(sshdPath.resolve(AVSTEM).resolve(AVSTEM_FIL))).isTrue();
		assertThat(Files.list(sshdPath.resolve(AVSTEM).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);

		Awaitility.await()
				.atMost(ofSeconds(500))
				.untilAsserted(() -> {
					assertThat(Files.list(sshdPath.resolve(AVSTEM).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(1);
				});

		List<String> processedMappe = Files.list(sshdPath.resolve(AVSTEM).resolve(PROCESSED))
				.map(p -> FilenameUtils.getName(p.toAbsolutePath().toString()))
				.collect(Collectors.toList());

		assertThat(processedMappe).containsExactly("04.11.2024_avstemmingsfil_1.txt");

		verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
		verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
		verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));

	}

	@Test
	public void shouldThrowExceptionJiraOppgaveForFeilendeAvstemReferanser() throws IOException {
		stubBadRequestJiraOpprettOppgave();

		final String AVSTEM_FIL = "04.11.2024_avstemmingsfil_1.txt";

		copyFileFromClasspathToAvstem(AVSTEM_FIL);

		assertThat(Files.exists(sshdPath.resolve(AVSTEM).resolve(AVSTEM_FIL))).isTrue();
		assertThat(Files.list(sshdPath.resolve(AVSTEM).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(0);

		Awaitility.await()
				.atMost(ofSeconds(500))
				.untilAsserted(() -> {
					assertThat(Files.list(sshdPath.resolve(AVSTEM).resolve(PROCESSED)).collect(Collectors.toSet())).hasSize(1);
				});

		verify(1, postRequestedFor(urlMatching(URL_DOKARKIV_AVSTEMREFERANSER)));
		verify(1, postRequestedFor(urlMatching(JIRA_OPPRETTE_URL)));
		verify(1, getRequestedFor(urlMatching(JIRA_PROJECT_URL)));

	}

	private void copyFileFromClasspathToAvstem(final String txtFilename) throws IOException {
		List<String> fileNames = Stream.of(new ClassPathResource(txtFilename).getContentAsString(UTF_8).split("\n")).toList();
		Files.copy(new ClassPathResource(txtFilename).getInputStream(), sshdPath.resolve(AVSTEM).resolve(txtFilename));
	}

	private void preparePath(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		} else {
			FileUtils.cleanDirectory(path.toFile());
		}
	}
}
