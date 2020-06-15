package no.nav.skanmotovrig.validator;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Bruker;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotovrig.metrics.Metrics;

import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

@Slf4j
public class SkanningMetadataValidator {

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "validate-ovrig"}, createErrorMetric = true)
    public static void validate(Skanningmetadata skanningmetadata) {
        verfiyMetadataIsValid(skanningmetadata);
    }

    private static void verfiyMetadataIsValid(Skanningmetadata skanningmetadata) {
        if (null == skanningmetadata) {
            throw new InvalidMetadataException("Skanningmetadata is null");
        }
        verifyJournalpostIsValid(skanningmetadata.getJournalpost());
        verifyBrukerisValid(skanningmetadata.getJournalpost().getBruker());
        verifySkanningInfoIsValid(skanningmetadata.getSkanningInfo());
    }

    private static void verifyBrukerisValid(Bruker bruker) {
        if(null != bruker){
            if(null == bruker.getBrukerId()){
                throw new InvalidMetadataException("BrukerId is null");
            }
            if(null == bruker.getBrukerType()){
                throw new InvalidMetadataException("BrukerType is null");
            }
            if(!BrukerValidator.isValidBrukerId(bruker.getBrukerId())) {
                throw new InvalidMetadataException("BrukerId is not valid");
            }
            if(!BrukerValidator.isValidBrukerType(bruker.getBrukerId(), bruker.getBrukerType())) {
                throw new InvalidMetadataException("Brukertype is not valid");
            }
        }
    }

    private static void verifyJournalpostIsValid(Journalpost journalpost) {
        if (null == journalpost) {
            throw new InvalidMetadataException("Journalpost is null");
        }
        if (!JournalpostValidator.isValidJournalfoerendeEnhet(journalpost.getJournalfoerendeEnhet())){
            throw new InvalidMetadataException("JournalfoerendeEnhet is not valid: " + journalpost.getJournalfoerendeEnhet());
        }
        if (!JournalpostValidator.isValidMottakskanal(journalpost.getMottakskanal())) {
            throw new InvalidMetadataException("Mottakskanal is not valid: " + journalpost.getMottakskanal());
        }
        if (!JournalpostValidator.isValidDatoMottatt(journalpost.getDatoMottatt())) {
            throw new InvalidMetadataException("DatoMottatt is not valid: " + journalpost.getDatoMottatt());
        }
        if (!JournalpostValidator.isValidBatchNavn(journalpost.getBatchNavn())) {
            throw new InvalidMetadataException("Batchnavn is not valid: " + journalpost.getBatchNavn());
        }
        if (!JournalpostValidator.isValidFilnavn(journalpost.getFilNavn())) {
            log.warn("Skanmotovrig Filnavn is not valid but generating journalpost anyway, filnavn={}", journalpost.getFilNavn());
        }
        if (!JournalpostValidator.isValidEndorsernr(journalpost.getEndorsernr())) {
            log.warn("Skanmotovrig Endorsernr is not valid but generating journalpost anyway, endorsernr={}, fil={}", journalpost.getEndorsernr(), journalpost.getFilNavn());
        }
        // Disse to feltene skal IKKE være i skanmotovrig
        if(!JournalpostValidator.isValidjournalpostId(journalpost.getJournalpostId())) {
            throw new InvalidMetadataException("Skanmotovrig JournalpostId is not valid: journalpostId should not be set. JournalpostId=" + journalpost.getJournalpostId());
        }
        if(!JournalpostValidator.isValidReferansenummer(journalpost.getReferansenummer())) {
            throw new InvalidMetadataException("Skanmotovrig Referansenummer is not valid: referansenummer should not be set. Referansenummer=" + journalpost.getReferansenummer());
        }
    }

    private static void verifySkanningInfoIsValid(SkanningInfo skanningInfo) {
        if (null == skanningInfo) {
            throw new InvalidMetadataException("SkanningInfo is null");
        }
        if (!SkanningInfoValidator.isValidFysiskPostboks(skanningInfo.getFysiskPostboks())) {
            log.warn("Skanmotovrig FysiskPostboks is not valid but generating journalpost anyway, FysiskPostboks={}", skanningInfo.getFysiskPostboks());
        }
        if (!SkanningInfoValidator.isValidStrekkodePostboks(skanningInfo.getStrekkodePostboks())) {
            log.warn("Skanmotovrig StrekkodePostboks is not valid but generating journalpost anyway, StrekkodePostboks={}", skanningInfo.getStrekkodePostboks());
        }
    }
}
