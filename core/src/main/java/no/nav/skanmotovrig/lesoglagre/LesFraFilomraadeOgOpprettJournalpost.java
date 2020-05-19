package no.nav.skanmotovrig.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigUnzipperFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.AbstractSkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.filomraade.FilomraadeService;
import no.nav.skanmotovrig.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotovrig.unzipskanningmetadata.Unzipper;
import no.nav.skanmotovrig.utils.Triple;
import no.nav.skanmotovrig.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LesFraFilomraadeOgOpprettJournalpost {

    private final FilomraadeService filomraadeService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public LesFraFilomraadeOgOpprettJournalpost(FilomraadeService filomraadeService,
                                                OpprettJournalpostService opprettJournalpostService) {
        this.filomraadeService = filomraadeService;
        this.opprettJournalpostService = opprettJournalpostService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 72 * HOUR)
    public void scheduledJob() {
        lesOgLagre();
    }

    public void lesOgLagre() {
        try{
            List<String> filenames = filomraadeService.getFileNames();
            log.info("Skanmotovrig fant {} zipfiler på sftp server", filenames.size());
            for(String zipName: filenames){
                log.info("Skanmotovrig laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList = Unzipper.unzipXmlPdf(filomraadeService.getZipFile(zipName));
                log.info("Skanmotovrig begynner behandling av {}", zipName);

                List<Triple<OpprettJournalpostResponse, Filepair, String>> responses = filepairList.stream()
                        .map(filepair -> opprettJournalpost(filepair))
                        .collect(Collectors.toList());

                boolean safeToDeleteZipFile = responses.stream()
                        .filter(triple -> triple.getT3() != null)
                        .anyMatch(invalid -> {
                            try {
                                String filename = invalid.getT2().getName();
                                String error = invalid.getT3();
                                log.warn(
                                        "Skanmotovrig laster opp {} fra {} til feilomrade, feilmelding={}",
                                        filename,
                                        zipName,
                                        error
                                );
                                lastOppFilpar(invalid.getT2(), zipName);
                                return true;
                            } catch (Exception e) {
                                log.error(
                                        "Skanmotovrig klarte ikke lagre {} til feilomrade, zipfil={}, feilmelding={}",
                                        invalid.getT2().getName(),
                                        zipName,
                                        e.getMessage()
                                );
                                return false;
                            }
                        });

                if(safeToDeleteZipFile) {
                    filomraadeService.moveZipFile(zipName, "processed");
                }
            }
        } catch(Exception e) {
            log.error("Skanmotovrig ukjent feil oppstod i lesOgLagre, feilmelding={}", e.getMessage(), e);
        }
    }

    private Triple<OpprettJournalpostResponse, Filepair, String> opprettJournalpost(Filepair filepair) {

        OpprettJournalpostResponse response = null;

        Triple<FilepairWithMetadata, Filepair, String> extractMetadataResult = extractMetadata(filepair);

        if (extractMetadataResult.getT3() != null) {
            return new Triple<>(null, filepair, extractMetadataResult.getT3());
        }
        try {
            log.info("Skanmotovrig oppretter journalpost for {}", filepair.getName());
            response = opprettJournalpostService.opprettJournalpost(extractMetadataResult.getT1());
            log.info("Skanmotovrig har opprettet journalpost for {}", filepair.getName());
        } catch (AbstractSkanmotovrigFunctionalException e) {
            log.error("Skanmotovrig feilet funskjonelt med oppretting av journalpost for {}", filepair.getName(), e);
            return new Triple<>(null, filepair, e.getMessage());
        } catch (AbstractSkanmotovrigTechnicalException e) {
            log.error("Skanmotovrig feilet teknisk med  oppretting av journalpost for {}", filepair.getName(), e);
            return new Triple<>(null, filepair, e.getMessage());
        }
        return new Triple<>(response, filepair, null);
    }

    private Triple<FilepairWithMetadata, Filepair, String> extractMetadata(Filepair filepair) {
        try {
            FilepairWithMetadata filepairWithMetadata = UnzipSkanningmetadataUtils.extractMetadata(filepair);
            return new Triple<>(filepairWithMetadata, filepair, null);
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotovrig klarte ikke unmarshalle.", filepair.getName(), e);
            return new Triple<>(null, filepair, e.getMessage());
        } catch (SkanmotovrigUnzipperFunctionalException e) {
            log.warn("Kunne ikke hente metadata fra {}, feilmelding={}",filepair.getName(), e.getMessage(), e);
            return new Triple<>(null, filepair, e.getMessage());
        }
    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
    }
}
