package no.nav.skanmotovrig.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
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

                List<Triple<OpprettJournalpostResponse, Filepair, Exception>> responses = filepairList.stream()
                        .map(filepair -> opprettJournalpost(filepair, zipName))
                        .collect(Collectors.toList());

                boolean safeToDeleteZipFile = responses.stream()
                        .filter(triple -> triple.getT3() != null)
                        .anyMatch(invalid -> {
                            try {
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
            log.info(e.getMessage());
        }
    }

    private Triple<OpprettJournalpostResponse, Filepair, Exception> opprettJournalpost(Filepair filepair, String zipName) {

        OpprettJournalpostResponse response = null;

        Triple<FilepairWithMetadata, Filepair, Exception> extractMetadataResult = extractMetadata(filepair, zipName);

        if (extractMetadataResult.getT3() != null) {
            return new Triple<>(null, filepair, extractMetadataResult.getT3());
        }
        try {
            response = opprettJournalpostService.opprettJournalpost(extractMetadataResult.getT1());
            log.info("Skanmotovrig lagret fildetaljer for journalpost");
        } catch (AbstractSkanmotovrigFunctionalException e) {
            log.error("Skanmotovrig feilet funskjonelt med lagring av fildetaljer til journalpost", e);
            return new Triple<>(null, filepair, e);
        } catch (AbstractSkanmotovrigTechnicalException e) {
            log.error("Skanmotovrig feilet teknisk med lagring av fildetaljer til journalpost", e);
            return new Triple<>(null, filepair, e);
        }
        return new Triple<>(response, filepair, null);
    }

    private Triple<FilepairWithMetadata, Filepair, Exception> extractMetadata(Filepair filepair, String zipName) {
        try {
            return new Triple<>(UnzipSkanningmetadataUtils.extractMetadata(filepair), filepair, null);
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotovrig klarte ikke unmarshalle.", filepair.getName(), e);
            return new Triple<>(null, filepair, e);
        }
    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
    }
}
