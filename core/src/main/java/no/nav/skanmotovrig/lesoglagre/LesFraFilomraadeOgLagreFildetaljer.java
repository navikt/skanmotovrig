package no.nav.skanmotovrig.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.AbstractSkanmotovrigTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.LagreFildetaljerService;
import no.nav.skanmotovrig.lagrefildetaljer.data.LagreFildetaljerResponse;
import no.nav.skanmotovrig.leszipfil.LesZipfilService;
import no.nav.skanmotovrig.utils.Unzipper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class LesFraFilomraadeOgLagreFildetaljer {

    private final LesZipfilService lesZipfilService;
    private final LagreFildetaljerService lagreFildetaljerService;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public LesFraFilomraadeOgLagreFildetaljer(LesZipfilService lesZipfilService,
                                              LagreFildetaljerService lagreFildetaljerService) {
        this.lesZipfilService = lesZipfilService;
        this.lagreFildetaljerService = lagreFildetaljerService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 72 * HOUR)
    public void scheduledJob() {
        tryToConnect();
    }

    public void tryToConnect(){
        try {
            List<byte[]> zipFiles = lesZipfilService.getZipFiles();
            zipFiles.stream()
                    .map(zipFile -> new ZipInputStream(new ByteArrayInputStream(zipFile)))
                    .forEach(this::logZipEntries);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void logZipEntries(ZipInputStream inputStream) {
        ZipEntry entry;
        try {
            while ((entry = inputStream.getNextEntry()) != null){
                log.info("ZipEntry = " + entry.getName());
            }
        } catch( Exception e ) {
            log.error("//TODO");
        }

    }

    public List<LagreFildetaljerResponse> lesOgLagre() {
        File zipfil = lesZipfilService.lesZipfil();
        try {
            List<FilepairWithMetadata> filepairWithMetadataList = Unzipper.unzipXmlPdf(zipfil);
            List<LagreFildetaljerResponse> responses = filepairWithMetadataList.stream()
                    .map(filepair -> lagreFil(filepair))
                    .filter(response -> null != response)
                    .collect(Collectors.toList());
            String zipName = filepairWithMetadataList.get(0).getSkanningmetadata().getJournalpost().getBatchNavn();
            log.info("skanmotovrig lagret fildetaljer fra zipfil {} i dokarkiv", zipName);
            return responses;
        } catch (IOException e) {
            log.error("skanmotovrig klarte ikke lese fra fil {}", zipfil.getName(), e);
            return null;
        }
    }

    private LagreFildetaljerResponse lagreFil(FilepairWithMetadata filepairWithMetadata) {
        LagreFildetaljerResponse response = null;
        try {
            response = lagreFildetaljerService.lagreFildetaljer(filepairWithMetadata);
            log.info("skanmotovrig lagret fildetaljer for journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId());
        } catch (AbstractSkanmotovrigFunctionalException e) {
            // TODO: Feilhåndtering
            log.error("skanmotovrig feilet funskjonelt med lagring av fildetaljer til journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), e);
        } catch (AbstractSkanmotovrigTechnicalException e) {
            // TODO: Feilhåndtering
            log.error("skanmotovrig feilet teknisk med lagring av fildetaljer til journalpost med id {}", filepairWithMetadata.getSkanningmetadata().getJournalpost().getJournalpostId(), e);
        }
        return response;
    }
}
