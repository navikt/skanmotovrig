package no.nav.skanmotovrig.validator;

import no.nav.skanmotovrig.domain.Bruker;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import org.junit.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidatorTest {
    private final String VALID_PERSONID = "***gammelt_fnr***";
    private final String VALID_ORGANISASJONID = "123456789";
    private final String VALID_PERSON = "PERSON";
    private final String VALID_ORGANISASJON = "ORGANISASJON";

    private final String INVALID_PERSONID = "***gammelt_fnr***0";
    private final String INVALID_ORGANISASJONID = "12345678";
    private final String INVALID_PERSON = "PESON";
    private final String INVALID_ORGANISASJON = "GANISASJON";

    private final String VALID_REFERANSENR = null;
    private final String VALID_JOURNALPOSTID = null;
    private final String VALID_BATCHNAVN = "BATCH";
    private final Date VALID_DATOMOTTATT = new Date();
    private final String VALID_ENDORSERNR = "ENDORSERNR";
    private final String VALID_FILNAVN = "FILNAVN.FILTYPE";
    private final String VALID_JOURNALFORENDEENHET = "1234";
    private final String VALID_MOTTAKSKANAL = "SKAN_IM";

    private final String INVALID_REFERANSENR = "123456789";
    private final String INVALID_JOURNALPOSTID = "123456789";
    private final String INVALID_BATCHNAVN = "";
    private final Date INVALID_DATOMOTTATT = null;
    private final String INVALID_ENDORSERNR = null;
    private final String INVALID_FILNAVN = "FILNAVN.";
    private final String INVALID_JOURNALFORENDEENHET = "OneTwoThreeFour";
    private final String INVALID_MOTTAKSKANAL = "SKAN_NETS";

    private final String VALID_FYSISKPOSTBOKS = "1111";
    private final String VALID_STREKKODEPOSTBOKS = "1423";

    private final String INVALID_FYSISKPOSTBOKS = "";
    private final String INVALID_STREKKODEPOSTBOKS = "1111";

    @Test
    public void brukerValidatorSholdAcceptValidData() {
        assertTrue(BrukerValidator.isValidBrukerId(VALID_PERSONID));
        assertTrue(BrukerValidator.isValidBrukerId(VALID_ORGANISASJONID));

        assertTrue(BrukerValidator.isValidBrukerType(VALID_PERSONID, VALID_PERSON));
        assertTrue(BrukerValidator.isValidBrukerType(VALID_ORGANISASJONID, VALID_ORGANISASJON));
    }

    @Test
    public void brukerValidatorShouldRejectInvalidData() {
        assertFalse(BrukerValidator.isValidBrukerId(INVALID_PERSONID));
        assertFalse(BrukerValidator.isValidBrukerId(INVALID_ORGANISASJONID));

        assertFalse(BrukerValidator.isValidBrukerType(INVALID_PERSONID, INVALID_PERSON));
        assertFalse(BrukerValidator.isValidBrukerType(INVALID_ORGANISASJONID, INVALID_ORGANISASJON));
    }

    @Test
    public void journalpostValidatorSholdAcceptValidData() {
        assertTrue(JournalpostValidator.isValidReferansenummer(VALID_REFERANSENR));
        assertTrue(JournalpostValidator.isValidjournalpostId(VALID_JOURNALPOSTID));
        assertTrue(JournalpostValidator.isValidBatchNavn(VALID_BATCHNAVN));
        assertTrue(JournalpostValidator.isValidDatoMottatt(VALID_DATOMOTTATT));
        assertTrue(JournalpostValidator.isValidEndorsernr(VALID_ENDORSERNR));
        assertTrue(JournalpostValidator.isValidFilnavn(VALID_FILNAVN));
        assertTrue(JournalpostValidator.isValidJournalfoerendeEnhet(VALID_JOURNALFORENDEENHET));
        assertTrue(JournalpostValidator.isValidMottakskanal(VALID_MOTTAKSKANAL));
    }

    @Test
    public void journalpostValidatorShouldRejectInvalidData() {
        assertFalse(JournalpostValidator.isValidReferansenummer(INVALID_REFERANSENR));
        assertFalse(JournalpostValidator.isValidjournalpostId(INVALID_JOURNALPOSTID));
        assertFalse(JournalpostValidator.isValidBatchNavn(INVALID_BATCHNAVN));
        assertFalse(JournalpostValidator.isValidDatoMottatt(INVALID_DATOMOTTATT));
        assertFalse(JournalpostValidator.isValidEndorsernr(INVALID_ENDORSERNR));
        assertFalse(JournalpostValidator.isValidFilnavn(INVALID_FILNAVN));
        assertFalse(JournalpostValidator.isValidJournalfoerendeEnhet(INVALID_JOURNALFORENDEENHET));
        assertFalse(JournalpostValidator.isValidMottakskanal(INVALID_MOTTAKSKANAL));
    }

    @Test
    public void skaninginfoValidatorSholdAcceptValidData() {
        assertTrue(SkanningInfoValidator.isValidFysiskPostboks(VALID_FYSISKPOSTBOKS));
        assertTrue(SkanningInfoValidator.isValidStrekkodePostboks(VALID_STREKKODEPOSTBOKS));
    }

    @Test
    public void skanninginfoValidatorShouldRejectInvalidData() {
        assertFalse(SkanningInfoValidator.isValidFysiskPostboks(INVALID_FYSISKPOSTBOKS));
        assertFalse(SkanningInfoValidator.isValidStrekkodePostboks(INVALID_STREKKODEPOSTBOKS));
    }

    @Test
    public void skanningMetadataValidatorSholdAcceptValidData() {
        Skanningmetadata skanningmetadata = Skanningmetadata.builder()
                .journalpost(Journalpost.builder()
                        .bruker(Bruker.builder()
                                .brukerType(VALID_PERSON)
                                .brukerId(VALID_PERSONID)
                                .build())
                        .batchNavn(VALID_BATCHNAVN)
                        .datoMottatt(VALID_DATOMOTTATT)
                        .endorsernr(VALID_ENDORSERNR)
                        .filNavn(VALID_FILNAVN)
                        .journalfoerendeEnhet(VALID_JOURNALFORENDEENHET)
                        .mottakskanal(VALID_MOTTAKSKANAL)
                        .build())
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks(VALID_FYSISKPOSTBOKS)
                        .strekkodePostboks(VALID_STREKKODEPOSTBOKS)
                        .build())
                .build();

        assertDoesNotThrow(() -> SkanningMetadataValidator.validate(skanningmetadata));
    }

    @Test
    public void skanningMetadataShouldRejectInvalidData() {
        Skanningmetadata skanningmetadata = Skanningmetadata.builder()
                .journalpost(Journalpost.builder()
                        .bruker(Bruker.builder()
                                .brukerType(INVALID_ORGANISASJON)
                                .brukerId(INVALID_PERSONID)
                                .build())
                        .journalpostId(INVALID_JOURNALPOSTID)
                        .referansenummer(INVALID_REFERANSENR)
                        .batchNavn(INVALID_BATCHNAVN)
                        .datoMottatt(INVALID_DATOMOTTATT)
                        .endorsernr(INVALID_ENDORSERNR)
                        .filNavn(INVALID_FILNAVN)
                        .journalfoerendeEnhet(INVALID_JOURNALFORENDEENHET)
                        .mottakskanal(INVALID_MOTTAKSKANAL)
                        .build())
                .skanningInfo(SkanningInfo.builder()
                        .fysiskPostboks(INVALID_FYSISKPOSTBOKS)
                        .strekkodePostboks(INVALID_STREKKODEPOSTBOKS)
                        .build())
                .build();

        assertThrows(InvalidMetadataException.class, () -> SkanningMetadataValidator.validate(skanningmetadata));
    }
}
