package no.nav.skanmotovrig.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigUnzipperFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.AbstractSkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.filomraade.FilomraadeService;
import no.nav.skanmotovrig.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotovrig.unzipskanningmetadata.Unzipper;
import no.nav.skanmotovrig.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class LesFraFilomraadeOgOpprettJournalpost {

    private final FilomraadeService filomraadeService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final int MINUTE = 60_000;

    public LesFraFilomraadeOgOpprettJournalpost(FilomraadeService filomraadeService,
                                                OpprettJournalpostService opprettJournalpostService) {
        this.filomraadeService = filomraadeService;
        this.opprettJournalpostService = opprettJournalpostService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 10 * MINUTE)
    public void scheduledJob() {
        lesOgLagre();
    }

    public void lesOgLagre() {
        try{
            List<String> filenames = filomraadeService.getFileNames();
            log.info("Skanmotovrig fant {} zipfiler på sftp server", filenames.size());
            for(String zipName: filenames){
                AtomicBoolean safeToDeleteZipFile = new AtomicBoolean(true);

                log.info("Skanmotovrig laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList = Unzipper.unzipXmlPdf(filomraadeService.getZipFile(zipName)); // TODO feilhåndtering hvis zipfil ikke er lesbar.
                log.info("Skanmotovrig begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                        Optional<OpprettJournalpostResponse> response = opprettJournalpost(filepair);
                        try {
                            if (response.isEmpty()){
                                log.warn("Skanmotovrig laster opp fil til feilområde fil={} zipFil={}", filepair.getName(), zipName);
                                lastOppFilpar(filepair, zipName);
                                log.warn("Skanmotovrig laster opp fil til feilområde fil={} zipFil={}", filepair.getName(), zipName);
                            }
                        } catch (Exception e) {
                            log.error("Skanmotovrig feilet ved opplasting til feilområde fil={} zipFil={} feilmelding={}", filepair.getName(), zipName, e.getMessage(), e);
                            safeToDeleteZipFile.set(false);
                        }
                });

                if(safeToDeleteZipFile.get()) {
                    filomraadeService.moveZipFile(zipName, "processed");
                }
            }
        } catch(Exception e) {
            log.error("Skanmotovrig ukjent feil oppstod i lesOgLagre, feilmelding={}", e.getMessage(), e);
        }
    }

    private Optional<OpprettJournalpostResponse> opprettJournalpost(Filepair filepair) {

        OpprettJournalpostResponse response = null;

        Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair);

        if (skanningmetadata.isEmpty()) {
            return Optional.empty();
        }
        try {
            log.info("Skanmotovrig oppretter journalpost for {}", filepair.getName());
            response = opprettJournalpostService.opprettJournalpost(skanningmetadata.get(), filepair);
            log.info("Skanmotovrig har opprettet journalpost, journalpostId={} fil={}", response.getJournalpostId(), filepair.getName());
        } catch (AbstractSkanmotovrigFunctionalException e) {
            log.error("Skanmotovrig feilet funskjonelt med oppretting av journalpost for {}", filepair.getName(), e);
            return Optional.empty();
        } catch (AbstractSkanmotovrigTechnicalException e) {
            log.error("Skanmotovrig feilet teknisk med  oppretting av journalpost for {}", filepair.getName(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Skanmotovrig feilet med ukjent feil ved oppretting av journalpost for {}", filepair.getName(), e);
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private Optional<Skanningmetadata> extractMetadata(Filepair filepair){
        try {
            return Optional.of(UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml()));
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotovrig klarte ikke unmarshalle.", filepair.getName(), e);
            return Optional.empty();
        } catch (SkanmotovrigUnzipperFunctionalException e) {
            log.warn("Kunne ikke hente metadata fra {}, feilmelding={}", filepair.getName(), e.getMessage(), e);
            return Optional.empty();
        }

    }

    private void lastOppFilpar(Filepair filepair, String zipName) {
        String path = Utils.removeFileExtensionInFilename(zipName);
        filomraadeService.uploadFileToFeilomrade(filepair.getPdf(), filepair.getName() + ".pdf", path);
        filomraadeService.uploadFileToFeilomrade(filepair.getXml(), filepair.getName() + ".xml", path);
    }
}
