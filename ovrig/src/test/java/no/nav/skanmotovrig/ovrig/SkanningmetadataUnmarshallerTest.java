package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException;
import no.nav.skanmotovrig.ovrig.domain.Bruker;
import no.nav.skanmotovrig.ovrig.domain.Journalpost;
import no.nav.skanmotovrig.ovrig.domain.SkanningInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;



public class SkanningmetadataUnmarshallerTest {
    @Test
    public void shouldMapFullXmlToDomainObject() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/OVRIG-20200529-1-1.xml";

        //SkanningInfo
        final String FYSISKPOSTBOKS = "1406";
        final String STREKKODEPOSTBOKS = "1406";
        //Journalpost
        final String TEMA = "PEN";
        final String BREVKODE = "Brevkode-1";
        final Bruker BRUKER = new Bruker("11111111111", "PERSON");
        final String JOURNALFORENDEENHET = "1234";
        final String MOTTAKSKANAL = "SKAN_IM";
        final LocalDate DATOMOTTATT = LocalDate.of(2020, 2, 22);
        final String BATCHNAVN = "OVRIG-20200529-1.zip";
        final String FILNAVN = "OVRIG-20200529-1-1.pdf";
        final String ENDORSERNR = "endorser-1";
        final String LAND = "DPK";
        final String ANTALLSIDER = "1";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));
        PostboksOvrigEnvelope envelope = new PostboksOvrigEnvelope(null, null, null, xmlFile, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        unmarshaller.unmarshal(envelope);

        SkanningInfo skanningInfo = envelope.getSkanningmetadata().getSkanningInfo();
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();

        assertThat(skanningInfo.getFysiskPostboks()).isEqualTo(FYSISKPOSTBOKS);
        assertThat(skanningInfo.getStrekkodePostboks()).isEqualTo(STREKKODEPOSTBOKS);

        assertThat(journalpost.getTema()).isEqualTo(TEMA);
        assertThat(journalpost.getBrevKode()).isEqualTo(BREVKODE);
        assertThat(journalpost.getBruker().getBrukerId()).isEqualTo(BRUKER.getBrukerId());
        assertThat(journalpost.getBruker().getBrukertype()).isEqualTo(BRUKER.getBrukertype());
        assertThat(journalpost.getJournalforendeEnhet()).isEqualTo(JOURNALFORENDEENHET);
        assertThat(journalpost.getMottakskanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(journalpost.getDatoMottatt().isEqual(DATOMOTTATT)).isEqualTo(true);
        assertThat(journalpost.getBatchnavn()).isEqualTo(BATCHNAVN);
        assertThat(journalpost.getFilnavn()).isEqualTo(FILNAVN);
        assertThat(journalpost.getEndorsernr()).isEqualTo(ENDORSERNR);
        assertThat(journalpost.getLand()).isEqualTo(LAND);
        assertThat(journalpost.getAntallSider()).isEqualTo(ANTALLSIDER);
    }

    @Test
    public void shouldMapMinimalXmlToDomainObject() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/OVRIG-20200529-1-2.xml";

        //SkanningInfo
        final String STREKKODEPOSTBOKS = "1406";
        //Journalpost
        final String MOTTAKSKANAL = "SKAN_IM";
        final LocalDate DATOMOTTATT = LocalDate.of(2020, 2, 22);
        final String BATCHNAVN = "OVRIG-20200529-1.zip";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));
        PostboksOvrigEnvelope envelope = new PostboksOvrigEnvelope(null, null, null, xmlFile, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        unmarshaller.unmarshal(envelope);

        SkanningInfo skanningInfo = envelope.getSkanningmetadata().getSkanningInfo();
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();

        assertThat(skanningInfo.getFysiskPostboks()).isEqualTo(null);
        assertThat(skanningInfo.getStrekkodePostboks()).isEqualTo(STREKKODEPOSTBOKS);

        assertThat(journalpost.getTema()).isEqualTo(null);
        assertThat(journalpost.getBrevKode()).isEqualTo(null);
        assertThat(journalpost.getBruker()).isEqualTo(null);
        assertThat(journalpost.getJournalforendeEnhet()).isEqualTo(null);
        assertThat(journalpost.getMottakskanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(journalpost.getDatoMottatt().isEqual(DATOMOTTATT)).isEqualTo(true);
        assertThat(journalpost.getBatchnavn()).isEqualTo(BATCHNAVN);
        assertThat(journalpost.getFilnavn()).isEqualTo(null);
        assertThat(journalpost.getEndorsernr()).isEqualTo(null);
        assertThat(journalpost.getLand()).isEqualTo(null);
        assertThat(journalpost.getAntallSider()).isEqualTo(null);
    }

    @Test
    public void shouldThrowWhenRequiredFieldsAreEmpty() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/OVRIG-20200529-1-3.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));

        PostboksOvrigEnvelope envelope = new PostboksOvrigEnvelope(null, null, null, xmlFile, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertThrows(
                SkanningmetadataValidationException.class,
                () -> unmarshaller.unmarshal(envelope)
        );
    }

    @Test
    public void shouldAcceptEmptyTagsInNonRequiredFields() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/OVRIG-20200529-1-4.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));
        PostboksOvrigEnvelope envelope = new PostboksOvrigEnvelope(null, null, null, xmlFile, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertDoesNotThrow(() -> unmarshaller.unmarshal(envelope));

        SkanningInfo skanningInfo = envelope.getSkanningmetadata().getSkanningInfo();
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();

        assertThat(skanningInfo.getFysiskPostboks()).isEqualTo("");

        assertThat(journalpost.getTema()).isEqualTo("");
        assertThat(journalpost.getBrevKode()).isEqualTo("");
        assertThat(journalpost.getJournalforendeEnhet()).isEqualTo("");
        assertThat(journalpost.getFilnavn()).isEqualTo("");
        assertThat(journalpost.getEndorsernr()).isEqualTo("");
        assertThat(journalpost.getLand()).isEqualTo("");
        assertThat(journalpost.getAntallSider()).isEqualTo("");
    }

    @Test
    public void shouldThrowWhenXmlisInvalid() throws IOException {
        final String XML_PATH = "src/test/resources/__files/xml/OVRIG-20200529-1-5.xml";

        byte[] xmlFile = Files.readAllBytes(Path.of(XML_PATH));

        PostboksOvrigEnvelope envelope = new PostboksOvrigEnvelope(null, null, null, xmlFile, null);
        SkanningmetadataUnmarshaller unmarshaller = new SkanningmetadataUnmarshaller();
        assertThrows(
                SkanningmetadataValidationException.class,
                () -> unmarshaller.unmarshal(envelope)
        );
    }

}
