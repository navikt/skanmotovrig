package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.exceptions.functional.ForsendelseNotCompleteException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.FILEBASENAME;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.OCR_FIL;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.PDF_FIL;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.XML_FIL;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.ZIPNAME;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.createBaseEnvelope;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.createEnvelopeWithOcr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class PostboksHelseEnvelopeTest {

    @Test
    void shouldThrowExceptionWhenValidateNoXml() {
        assertThrows(ForsendelseNotCompleteException.class, () -> {
            createBaseEnvelope().xml(null).build()
                    .validate();
        }, "Fant ikke filnavn=" + FILEBASENAME + ".xml i zip=" + ZIPNAME);
    }

    @Test
    void shouldThrowExceptionWhenValidateNoPdf() {
        assertThrows(ForsendelseNotCompleteException.class, () -> {
            createBaseEnvelope().pdf(null).build()
                    .validate();
        }, "Fant ikke filnavn=" + FILEBASENAME + ".pdf i zip=" + ZIPNAME);
    }

    @Test
    void shouldCreateZip() throws IOException, ArchiveException {
        final PostboksHelseEnvelope envelope = createEnvelopeWithOcr();
        ByteArrayInputStream zip = (ByteArrayInputStream) envelope.createZip();
        SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(zip.readAllBytes());
        ZipFile zipFile = new ZipFile(inMemoryByteChannel);
        assertThat(readEntry(zipFile, FILEBASENAME + ".xml")).containsExactly(XML_FIL);
        assertThat(readEntry(zipFile, FILEBASENAME + ".pdf")).containsExactly(PDF_FIL);
        assertThat(readEntry(zipFile, FILEBASENAME + ".ocr")).containsExactly(OCR_FIL);
    }

    private byte[] readEntry(final ZipFile zipFile, final String name) throws IOException {
        ZipArchiveEntry archiveEntry = zipFile.getEntry(name);
        InputStream inputStream = zipFile.getInputStream(archiveEntry);
        return IOUtils.toByteArray(inputStream);
    }
}