package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException;
import no.nav.skanmotovrig.helse.domain.Bruker;
import no.nav.skanmotovrig.helse.domain.Journalpost;
import no.nav.skanmotovrig.helse.domain.Skanninginfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SkanningmetadataUnmarshallerTest {
    @Test
    public void shouldMapFullXmlToDomainObject() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-1.xml";

        //SkanningInfo
        final String FYSISKPOSTBOKS = "1411";
        final String STREKKODEPOSTBOKS = "1411";
        //Journalpost
        final Bruker BRUKER = new Bruker("***gammelt_fnr***", "PERSON");
        final String MOTTAKSKANAL = "SKAN_IM";
        final LocalDate DATOMOTTATT = LocalDate.of(2020, 6, 4);
        final String BATCHNAVN = "BHELSE-20200529-1.zip";
        final String FILNAVN = "BHELSE-20200529-1-1.pdf";
        final String ENDORSERNR = "string";
        final String ANTALLSIDER = "1";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));
        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        unmarshaller.unmarshal(envelope);

        Skanninginfo skanninginfo = envelope.getSkanningmetadata().getSkanninginfo();
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();

        assertThat(skanninginfo.getFysiskPostboks()).isEqualTo(FYSISKPOSTBOKS);
        assertThat(skanninginfo.getStrekkodePostboks()).isEqualTo(STREKKODEPOSTBOKS);

        assertThat(journalpost.getBruker().getBrukerId()).isEqualTo(BRUKER.getBrukerId());
        assertThat(journalpost.getBruker().getBrukerType()).isEqualTo(BRUKER.getBrukerType());
        assertThat(journalpost.getMottakskanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(journalpost.getDatoMottatt().isEqual(DATOMOTTATT)).isEqualTo(true);
        assertThat(journalpost.getBatchnavn()).isEqualTo(BATCHNAVN);
        assertThat(journalpost.getFilNavn()).isEqualTo(FILNAVN);
        assertThat(journalpost.getEndorsernr()).isEqualTo(ENDORSERNR);
        assertThat(journalpost.getAntallSider()).isEqualTo(ANTALLSIDER);
    }

    @Test
    public void shouldMapMinimalXmlToDomainObject() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-2.xml";

        //SkanningInfo
        final String FYSISKPOSTBOKS = "1411";
        final String STREKKODEPOSTBOKS = "1411";
        //Journalpost
        final String MOTTAKSKANAL = "SKAN_IM";
        final LocalDate DATOMOTTATT = LocalDate.of(2020, 6, 4);
        final String BATCHNAVN = "BHELSE-20200529-1.zip";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));
        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        unmarshaller.unmarshal(envelope);

        Skanninginfo skanninginfo = envelope.getSkanningmetadata().getSkanninginfo();
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();

        assertThat(skanninginfo.getFysiskPostboks()).isEqualTo(FYSISKPOSTBOKS);
        assertThat(skanninginfo.getStrekkodePostboks()).isEqualTo(STREKKODEPOSTBOKS);

        assertThat(journalpost.getBruker()).isEqualTo(null);
        assertThat(journalpost.getMottakskanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(journalpost.getDatoMottatt().isEqual(DATOMOTTATT)).isEqualTo(true);
        assertThat(journalpost.getBatchnavn()).isEqualTo(BATCHNAVN);
        assertThat(journalpost.getFilNavn()).isEqualTo(null);
        assertThat(journalpost.getEndorsernr()).isEqualTo(null);
        assertThat(journalpost.getAntallSider()).isEqualTo(null);
    }

    @Test
    public void shouldThrowIfRequiredFieldsAreMissing() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-3.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));

        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertThrows(
                SkanningmetadataValidationException.class,
                () -> unmarshaller.unmarshal(envelope)
        );
    }

    @Test
    public void shouldThrowIfRequiredFieldsAreEmpty() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-4.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));

        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertThrows(
                SkanningmetadataValidationException.class,
                () -> unmarshaller.unmarshal(envelope)
        );
    }

    @Test
    public void shouldMapEmptyOptionalTagsToDomainObject() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-5.xml";

        //SkanningInfo
        final String FYSISKPOSTBOKS = "1411";
        final String STREKKODEPOSTBOKS = "1411";
        //Journalpost
        final Bruker BRUKER = new Bruker("***gammelt_fnr***", "PERSON");
        final String MOTTAKSKANAL = "SKAN_IM";
        final LocalDate DATOMOTTATT = LocalDate.of(2020, 6, 4);
        final String BATCHNAVN = "BHELSE-20200529-1.zip";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));
        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        unmarshaller.unmarshal(envelope);

        Skanninginfo skanninginfo = envelope.getSkanningmetadata().getSkanninginfo();
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();

        assertThat(skanninginfo.getFysiskPostboks()).isEqualTo(FYSISKPOSTBOKS);
        assertThat(skanninginfo.getStrekkodePostboks()).isEqualTo(STREKKODEPOSTBOKS);

        assertThat(journalpost.getBruker().getBrukerId()).isEqualTo(BRUKER.getBrukerId());
        assertThat(journalpost.getBruker().getBrukerType()).isEqualTo(BRUKER.getBrukerType());
        assertThat(journalpost.getMottakskanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(journalpost.getDatoMottatt().isEqual(DATOMOTTATT)).isEqualTo(true);
        assertThat(journalpost.getBatchnavn()).isEqualTo(BATCHNAVN);
        assertThat(journalpost.getFilNavn()).isEqualTo("");
        assertThat(journalpost.getEndorsernr()).isEqualTo("");
        assertThat(journalpost.getAntallSider()).isEqualTo("");
    }

    @Test
    public void shouldThrowIfInvalidXml() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-6.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));

        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertThrows(
                SkanningmetadataValidationException.class,
                () -> unmarshaller.unmarshal(envelope)
        );
    }

    @Test
    public void shouldThrowIfInvalidValuesInFields() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/BHELSE-20200529-1-7.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));

        PostboksHelseEnvelope envelope = new PostboksHelseEnvelope(null, null, null, xmlFile, null, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertThrows(
                SkanningmetadataValidationException.class,
                () -> unmarshaller.unmarshal(envelope)
        );
    }
}
