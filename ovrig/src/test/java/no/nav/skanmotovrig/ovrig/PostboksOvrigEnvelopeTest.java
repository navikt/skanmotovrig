package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.exceptions.functional.ForsendelseNotCompleteException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class PostboksOvrigEnvelopeTest {

    @Test
    void shouldThrowExceptionWhenValidateNoXml() {
        Assertions.assertThrows(ForsendelseNotCompleteException.class, () -> {
            PostboksOvrigEnvelopeTestObjects.createFullEnvelope().xml(null).build()
                    .validate();
        }, "Fant ikke filnavn=" + PostboksOvrigEnvelopeTestObjects.FILEBASENAME + ".xml i zip=" + PostboksOvrigEnvelopeTestObjects.ZIPNAME);
    }

    @Test
    void shouldThrowExceptionWhenValidateNoPdf() {
        Assertions.assertThrows(ForsendelseNotCompleteException.class, () -> {
            PostboksOvrigEnvelopeTestObjects.createFullEnvelope().pdf(null).build()
                    .validate();
        }, "Fant ikke filnavn=" + PostboksOvrigEnvelopeTestObjects.FILEBASENAME + ".pdf i zip=" + PostboksOvrigEnvelopeTestObjects.ZIPNAME);
    }

    @Test
    void shouldCreateZip() throws IOException, ArchiveException {
        final PostboksOvrigEnvelope envelope = PostboksOvrigEnvelopeTestObjects.createFullEnvelope().build();
        ByteArrayInputStream zip = (ByteArrayInputStream) envelope.createZip();
        SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(zip.readAllBytes());
        ZipFile zipFile = new ZipFile(inMemoryByteChannel);
        assertThat(readEntry(zipFile, PostboksOvrigEnvelopeTestObjects.FILEBASENAME + ".xml")).containsExactly(PostboksOvrigEnvelopeTestObjects.XML_FIL);
        assertThat(readEntry(zipFile, PostboksOvrigEnvelopeTestObjects.FILEBASENAME + ".pdf")).containsExactly(PostboksOvrigEnvelopeTestObjects.PDF_FIL);
    }

    private byte[] readEntry(final ZipFile zipFile, final String name) throws IOException {
        ZipArchiveEntry archiveEntry = zipFile.getEntry(name);
        InputStream inputStream = zipFile.getInputStream(archiveEntry);
        return IOUtils.toByteArray(inputStream);
    }
}