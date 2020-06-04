package no.nav.skanmotovrig.unittest;

import no.nav.skanmotovrig.domain.Bruker;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotovrig.validator.BrukerValidator;
import no.nav.skanmotovrig.validator.JournalpostValidator;
import no.nav.skanmotovrig.validator.SkanningInfoValidator;
import no.nav.skanmotovrig.validator.SkanningMetadataValidator;
import org.junit.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ValidatorTest {
    @Test
    public void brukerValidatorSholdAcceptValidData(){
        String PERSONID = "***gammelt_fnr***";
        String ORGANISASJONID = "123456789";
        String PERSON = "PERSON";
        String ORGANISASJON = "ORGANISASJON";

        assertTrue(BrukerValidator.isValidBrukerId(PERSONID));
        assertTrue(BrukerValidator.isValidBrukerId(ORGANISASJONID));

        assertTrue(BrukerValidator.isValidBrukerType(PERSONID, PERSON));
        assertTrue(BrukerValidator.isValidBrukerType(ORGANISASJONID, ORGANISASJON));
    }
    @Test
    public void brukerValidatorShouldRejectInvalidData(){
        String PERSONID = "***gammelt_fnr***0";
        String ORGANISASJONID = "12345678";
        String PERSON = "PESON";
        String ORGANISASJON = "GANISASJON";

        assertFalse(BrukerValidator.isValidBrukerId(PERSONID));
        assertFalse(BrukerValidator.isValidBrukerId(ORGANISASJONID));

        assertFalse(BrukerValidator.isValidBrukerType(PERSONID, PERSON));
        assertFalse(BrukerValidator.isValidBrukerType(ORGANISASJONID, ORGANISASJON));
    }
    @Test
    public void journalpostValidatorSholdAcceptValidData(){
        String REFERANSENR = null;
        String JOURNALPOSTID = null;
        String BATCHNAVN = "BATCH";
        Date DATOMOTTATT = new Date();
        String ENDORSERNR = "ENDORSERNR";
        String FILNAVN = "FILNAVN.FILTYPE";
        String JOURNALFORENDEENHET = "1234";
        String MOTTAKSKANAL = "SKAN_IM";

        assertTrue(JournalpostValidator.isValidReferansenummer(REFERANSENR));
        assertTrue(JournalpostValidator.isValidjournalpostId(JOURNALPOSTID));
        assertTrue(JournalpostValidator.isValidBatchNavn(BATCHNAVN));
        assertTrue(JournalpostValidator.isValidDatoMottatt(DATOMOTTATT));
        assertTrue(JournalpostValidator.isValidEndorsernr(ENDORSERNR));
        assertTrue(JournalpostValidator.isValidFilnavn(FILNAVN));
        assertTrue(JournalpostValidator.isValidJournalfoerendeEnhet(JOURNALFORENDEENHET));
        assertTrue(JournalpostValidator.isValidMottakskanal(MOTTAKSKANAL));
    }
    @Test
    public void journalpostValidatorShouldRejectInvalidData(){
        String REFERANSENR = "123456789";
        String JOURNALPOSTID = "123456789";
        String BATCHNAVN = "";
        Date DATOMOTTATT = null;
        String ENDORSERNR = null;
        String FILNAVN = "FILNAVN.";
        String JOURNALFORENDEENHET = "OneTwoThreeFour";
        String MOTTAKSKANAL = "SKAN_NETS";

        assertFalse(JournalpostValidator.isValidReferansenummer(REFERANSENR));
        assertFalse(JournalpostValidator.isValidjournalpostId(JOURNALPOSTID));
        assertFalse(JournalpostValidator.isValidBatchNavn(BATCHNAVN));
        assertFalse(JournalpostValidator.isValidDatoMottatt(DATOMOTTATT));
        assertFalse(JournalpostValidator.isValidEndorsernr(ENDORSERNR));
        assertFalse(JournalpostValidator.isValidFilnavn(FILNAVN));
        assertFalse(JournalpostValidator.isValidJournalfoerendeEnhet(JOURNALFORENDEENHET));
        assertFalse(JournalpostValidator.isValidMottakskanal(MOTTAKSKANAL));
    }
    @Test
    public void skaninginfoValidatorSholdAcceptValidData(){
        String FYSISKPOSTBOKS = "1111";
        String STREKKODEPOSTBOKS = "1423";

        assertTrue(SkanningInfoValidator.isValidFysiskPostboks(FYSISKPOSTBOKS));
        assertTrue(SkanningInfoValidator.isValidStrekkodePostboks(STREKKODEPOSTBOKS));
    }
    @Test
    public void skanninginfoValidatorShouldRejectInvalidData(){
        String FYSISKPOSTBOKS = "";
        String STREKKODEPOSTBOKS = "1111";

        assertFalse(SkanningInfoValidator.isValidFysiskPostboks(FYSISKPOSTBOKS));
        assertFalse(SkanningInfoValidator.isValidStrekkodePostboks(STREKKODEPOSTBOKS));
    }

    @Test
    public void skanningMetadataValidatorSholdAcceptValidData(){
        String PERSONID = "***gammelt_fnr***";
        String PERSON = "PERSON";

        String BATCHNAVN = "BATCH";
        Date DATOMOTTATT = new Date();
        String ENDORSERNR = "ENDORSERNR";
        String FILNAVN = "FILNAVN.FILTYPE";
        String JOURNALFORENDEENHET = "1234";
        String MOTTAKSKANAL = "SKAN_IM";

        String FYSISKPOSTBOKS = "1111";
        String STREKKODEPOSTBOKS = "1423";

        Skanningmetadata skanningmetadata = Skanningmetadata.builder()
                .journalpost(Journalpost.builder()
                        .bruker(Bruker.builder()
                                .brukerType(PERSON)
                                .brukerId(PERSONID)
                                .build())
                        .batchNavn(BATCHNAVN)
                        .datoMottatt(DATOMOTTATT)
                        .endorsernr(ENDORSERNR)
                        .filNavn(FILNAVN)
                        .journalfoerendeEnhet(JOURNALFORENDEENHET)
                        .mottakskanal(MOTTAKSKANAL)
                        .build())
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks(FYSISKPOSTBOKS)
                        .strekkodePostboks(STREKKODEPOSTBOKS)
                        .build())
                .build();

        assertDoesNotThrow(() -> SkanningMetadataValidator.validate(skanningmetadata));
    }
    @Test
    public void skanningMetadataShouldRejectInvalidData(){
        String PERSONID = "***gammelt_fnr***";
        String ORGANISASJON = "ORGANISASJON";

        String REFERANSENR = "123456789";
        String JOURNALPOSTID = "123456789";
        String BATCHNAVN = null;
        Date DATOMOTTATT = null;
        String ENDORSERNR = "";
        String FILNAVN = "FILNAVN.";
        String JOURNALFORENDEENHET = "OneTwoThreeFour";
        String MOTTAKSKANAL = "SKAN_NETS";

        String FYSISKPOSTBOKS = "";
        String STREKKODEPOSTBOKS = "1111";

        Skanningmetadata skanningmetadata = Skanningmetadata.builder()
                .journalpost(Journalpost.builder()
                        .bruker(Bruker.builder()
                                .brukerType(ORGANISASJON)
                                .brukerId(PERSONID)
                                .build())
                        .journalpostId(JOURNALPOSTID)
                        .referansenummer(REFERANSENR)
                        .batchNavn(BATCHNAVN)
                        .datoMottatt(DATOMOTTATT)
                        .endorsernr(ENDORSERNR)
                        .filNavn(FILNAVN)
                        .journalfoerendeEnhet(JOURNALFORENDEENHET)
                        .mottakskanal(MOTTAKSKANAL)
                        .build())
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks(FYSISKPOSTBOKS)
                        .strekkodePostboks(STREKKODEPOSTBOKS)
                        .build())
                .build();

        assertThrows(InvalidMetadataException.class, () -> SkanningMetadataValidator.validate(skanningmetadata));
    }
}
