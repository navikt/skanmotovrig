package no.nav.skanmotovrig.itest;

import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;
import no.nav.skanmotovrig.itest.config.TestConfig;
import no.nav.skanmotovrig.sftp.Sftp;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.server.SshServer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import wiremock.org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@ActiveProfiles("itest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {TestConfig.class})
public class SftpTestIT {

    private static final String INNGAAENDE = "inngaaende";

    private static final String ZIP_FILE_NAME = "01.07.2020_R123456789_1_1000.zip";
    private static final String ZIP_FILE_PATH = "__files/" + ZIP_FILE_NAME;
    private static final String XML_FILE_PATH = "__files/data_002.xml";
    private static final String DIR_ONE = "dirOne";
    private static final String DIR_TWO = "dirTwo";

    private Sftp sftp;

    @Autowired
    SkanmotovrigProperties skanmotovrigeProperties;

    @Inject
    private Path sshdPath;

    @Inject
    private SshServer sshd;

    @BeforeAll
    void beforeAll() {
        sftp = new Sftp(skanmotovrigeProperties);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        final Path inngaaende = sshdPath.resolve(INNGAAENDE);
        final Path dir1 = sshdPath.resolve(DIR_ONE);
        final Path dir2 = sshdPath.resolve(DIR_TWO);
        preparePath(inngaaende);
        preparePath(dir1);
        preparePath(dir2);

        moveFilesToDirectory();
    }

    @Test
    public void shouldConnectAndReconnectToSftp() {
        try {
            PropertyResolverUtils.updateProperty(sshd, FactoryManager.IDLE_TIMEOUT, 2000L);

            sftp.listFiles();
            Assert.assertTrue(sftp.isConnected());
            Assert.assertEquals(1, sshd.getActiveSessions().size());
            Assert.assertEquals("itestUser", sshd.getActiveSessions().iterator().next().getUsername());

            Thread.sleep(3000);
            Assert.assertFalse(sftp.isConnected());

            sftp.listFiles();
            Assert.assertTrue(sftp.isConnected());
            Assert.assertEquals(1, sshd.getActiveSessions().size());
            Assert.assertEquals("itestUser", sshd.getActiveSessions().iterator().next().getUsername());
        } catch (Exception e) {
            Assert.fail();
        } finally {
            PropertyResolverUtils.updateProperty(sshd, FactoryManager.IDLE_TIMEOUT, 60000L);
        }
    }

    @Test
    void shouldChangeDirectoryAndListFiles() {
        try {
            sftp.changeDirectory(sftp.getHomePath() + DIR_ONE);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + DIR_ONE));
            Assert.assertTrue(sftp.listFiles().contains("fileOne"));

            sftp.changeDirectory(sftp.getHomePath() + DIR_TWO);
            Assert.assertTrue(sftp.presentWorkingDirectory().endsWith(sftp.getHomePath() + DIR_TWO));
            Assert.assertTrue(sftp.listFiles().contains("fileTwo"));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldFailToChangeDirectoryToInvalidPath() {
        try {
            sftp.changeDirectory("ikke/en/gyldig/path");
            Assert.fail();
        } catch (SkanmotovrigSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("failed to change directory, path: ikke/en/gyldig/path", e.getMessage());
        } catch (Exception e) {
            sftp.disconnect();
            Assert.fail();
        }
    }

    @Test
    void shouldGetFile() {
        try {
            sftp.changeDirectory(INNGAAENDE);

            InputStream inputStream = sftp.getFile(ZIP_FILE_NAME);
            Assert.assertArrayEquals(new ClassPathResource(ZIP_FILE_PATH).getInputStream().readAllBytes(), inputStream.readAllBytes());

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToGetFileWhenFileNameIsInvalid() {
        try {
            sftp.changeDirectory(INNGAAENDE);
            sftp.getFile("nonExistingFile.zip");

            Assert.fail();
        } catch (SkanmotovrigSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("failed to download nonExistingFile.zip", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldDeleteFile() {
        File f = new File(sshdPath.resolve(INNGAAENDE).resolve("tmpfil.txt").toUri());
        try {
            int initialNumberOfFiles = sftp.listFiles(INNGAAENDE).size();

            f.createNewFile();
            Assert.assertEquals(initialNumberOfFiles + 1, sftp.listFiles(INNGAAENDE).size());
            sftp.deleteFile(INNGAAENDE, "tmpfil.txt");

            Assert.assertEquals(initialNumberOfFiles, sftp.listFiles(INNGAAENDE).size());

            sftp.disconnect();
        } catch (Exception e) {
            f.delete();
            Assert.fail();
        }
    }

    @Test
    void shouldFailToDeleteNonExistingFile() {
        try {
            sftp.deleteFile(INNGAAENDE, "nonExistingFile.txt");

            Assert.fail();
        } catch (SkanmotovrigSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke slette " + INNGAAENDE + "/nonExistingFile.txt", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldFailToDeleteNonExistingPath() {
        try {
            sftp.deleteFile("ikke/en/gyldig/path", "nonExistingFile.txt");

            Assert.fail();
        } catch (SkanmotovrigSftpTechnicalException e) {
            sftp.disconnect();
            Assert.assertEquals("Klarte ikke slette ikke/en/gyldig/path/nonExistingFile.txt", e.getMessage());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldUploadFile() {
        try {
            InputStream zipFile = new ClassPathResource(ZIP_FILE_PATH).getInputStream();
            String filename = "uploadedFile.xml";

            sftp.uploadFile(zipFile, INNGAAENDE, filename);

            Assert.assertTrue(sftp.listFiles(INNGAAENDE).contains(filename));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldUploadFileToNewDirectory() {
        try {
            InputStream xmlFile = new ClassPathResource(XML_FILE_PATH).getInputStream();
            String filename = "uploadedFile.xml";

            sftp.uploadFile(xmlFile, INNGAAENDE + "/newDirectory", filename);

            Assert.assertTrue(sftp.listFiles(INNGAAENDE + "/newDirectory/").contains(filename));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    void shouldMoveFile() {
        try {
            File f = new File(sshdPath.resolve(DIR_ONE).resolve("tmpFile.txt").toString());
            f.createNewFile();

            Assert.assertFalse(sftp.listFiles(DIR_TWO).contains("movedFile.txt"));

            sftp.moveFile(DIR_ONE + "/tmpFile.txt", DIR_TWO, "movedFile.txt");

            Assert.assertTrue(sftp.listFiles(DIR_TWO).contains("movedFile.txt"));

            sftp.disconnect();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private void preparePath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        } else {
            FileUtils.cleanDirectory(path.toFile());
        }
    }

    private void moveFilesToDirectory() throws IOException {
        Files.copy(new ClassPathResource("sftp/" + DIR_ONE + "/fileOne").getInputStream(), sshdPath.resolve(DIR_ONE).resolve("fileOne"));
        Files.copy(new ClassPathResource("sftp/" + DIR_TWO + "/fileTwo").getInputStream(), sshdPath.resolve(DIR_TWO).resolve("fileTwo"));
        Files.copy(new ClassPathResource(ZIP_FILE_PATH).getInputStream(), sshdPath.resolve(INNGAAENDE).resolve(ZIP_FILE_NAME));
    }
}
