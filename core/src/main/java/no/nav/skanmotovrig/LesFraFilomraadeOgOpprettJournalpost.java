package no.nav.skanmotovrig;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigUnzipperFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.AbstractSkanmotovrigTechnicalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigUnzipperTechnicalException;
import no.nav.skanmotovrig.filomraade.FilomraadeService;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.mdc.MDCGenerate;
import no.nav.skanmotovrig.metrics.DokCounter;
import no.nav.skanmotovrig.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotovrig.unzipskanningmetadata.Unzipper;
import no.nav.skanmotovrig.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Slf4j
@Component
public class LesFraFilomraadeOgOpprettJournalpost {

    private final FilomraadeService filomraadeService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final DokCounter dokCounter;

    private final String TEMA = "tema";
    private final String STREKKODEPOSTBOKS = "strekkodePostboks";
    private final String FYSISKPOSTBOKS = "fysiskPostboks";
    private final String EMPTY = "empty";


    public LesFraFilomraadeOgOpprettJournalpost(FilomraadeService filomraadeService,
                                                OpprettJournalpostService opprettJournalpostService,
                                                DokCounter dokCounter
    ) {
        this.filomraadeService = filomraadeService;
        this.opprettJournalpostService = opprettJournalpostService;
        this.dokCounter = dokCounter;
    }

    @Scheduled(cron = "${skanmotovrig.ovrig.schedule}")
    public void scheduledJob() {
        lesOgLagre();
    }

    public String lesOgLagre() {
        int numberOfZipfiles = 0;
        int numberOfFilePairs = 0;
        AtomicInteger numberOfFailingFilePairs = new AtomicInteger(0);

        try {
            List<String> filenames = filomraadeService.getFileNames();

            numberOfZipfiles = filenames.size();

            log.info("Skanmotovrig fant {} zipfiler på sftp server", filenames.size());
            for (String zipName : filenames) {
                setUpMDCforZip(zipName);
                AtomicBoolean safeToDeleteZipFile = new AtomicBoolean(true);

                log.info("Skanmotovrig laster ned {} fra sftp server", zipName);
                List<Filepair> filepairList;
                try {
                    filepairList = Unzipper.unzipXmlPdf(filomraadeService.getZipFile(zipName));
                } catch (Exception e) {
                    filepairList = List.of();
                    safeToDeleteZipFile.set(false);
                    incrementErrorMetrics(e);
                }
                numberOfFilePairs += filepairList.size();

                log.info("Skanmotovrig begynner behandling av {}", zipName);

                filepairList.forEach(filepair -> {
                    setUpMDCforFile(filepair.getName());

                    Optional<OpprettJournalpostResponse> response = opprettJournalpost(filepair);

                    if (response.isEmpty()) {
                        numberOfFailingFilePairs.getAndIncrement();
                        boolean opplastingOk = lastOppFilpar(filepair, zipName);
                        safeToDeleteZipFile.set(opplastingOk && safeToDeleteZipFile.get());
                    }
                    tearDownMDCforFile();

                });

                if (safeToDeleteZipFile.get()) {
                    filomraadeService.moveZipFile(zipName, "processed");
                }
                tearDownMDCforZip();
            }
        } catch (Exception e) {
            log.error("Skanmotovrig ukjent feil oppstod i lesOgLagre, feilmelding={}", e.getMessage(), e);
            incrementErrorMetrics(e);
        } finally {
            // Feels like a leaky abstraction ...
            filomraadeService.disconnect();
        }
        log.info("#ZIPFILES={}, #FILEPAIRS={}, #FAILING={}", numberOfZipfiles, numberOfFilePairs, numberOfFailingFilePairs.get());
        return "#ZIPFILES=" + numberOfZipfiles + ", #FILEPAIRS=" + numberOfFilePairs + ", #FAILING=" + numberOfFailingFilePairs.get();
    }

    private Optional<OpprettJournalpostResponse> opprettJournalpost(Filepair filepair) {

        OpprettJournalpostResponse response;

        Optional<Skanningmetadata> skanningmetadata = extractMetadata(filepair);


        if (skanningmetadata.isEmpty()) {
            return Optional.empty();
        }

        String batchNavn = skanningmetadata.map(Skanningmetadata::getJournalpost).map(Journalpost::getBatchNavn).orElse(null);
        try {
            log.info("Skanmotovrig oppretter journalpost fil={}, batch={}", filepair.getName(), batchNavn);
            response = opprettJournalpostService.opprettJournalpost(skanningmetadata.get(), filepair);
            log.info("Skanmotovrig har opprettet journalpost, journalpostId={}, fil={}, batch={}", response.getJournalpostId(), filepair.getName(), batchNavn);
        } catch (AbstractSkanmotovrigFunctionalException e) {
            log.error("Skanmotovrig feilet funskjonelt med oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            incrementErrorMetrics(e);
            return Optional.empty();
        } catch (AbstractSkanmotovrigTechnicalException e) {
            log.error("Skanmotovrig feilet teknisk med  oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            incrementErrorMetrics(e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Skanmotovrig feilet med ukjent feil ved oppretting av journalpost fil={}, batch={}", filepair.getName(), batchNavn, e);
            incrementErrorMetrics(e);
            return Optional.empty();
        }
        return Optional.of(response);
    }

    private Optional<Skanningmetadata> extractMetadata(Filepair filepair) {
        if (filepair.getPdf() == null) {
            Exception e = new InvalidMetadataException("Mangler fysisk dokument");
            log.error("Filpar mangler fysisk dokument for fil {}.", filepair.getName(), e);
            incrementErrorMetrics(e);
            return Optional.empty();
        }
        try {
            Skanningmetadata skanningmetadata = UnzipSkanningmetadataUtils.bytesToSkanningmetadata(filepair.getXml());
            incrementMetadataMetrics(skanningmetadata);
            skanningmetadata.verifyFields();

            return Optional.of(skanningmetadata);
        } catch (InvalidMetadataException e) {
            log.error("Skanningmetadata hadde ugyldige verdier for fil {}. Skanmotovrig klarte ikke unmarshalle.", filepair.getName(), e);
            incrementErrorMetrics(e);
            return Optional.empty();
        } catch (SkanmotovrigUnzipperFunctionalException e) {
            log.error("Kunne ikke hente metadata fra {}, feilmelding={}", filepair.getName(), e.getMessage(), e);
            incrementErrorMetrics(e);
            return Optional.empty();
        } catch (SkanmotovrigUnzipperTechnicalException | NullPointerException e) {
            log.error("Teknisk feil oppstod ved deserialisering av {}, feilmelding={}, cause={}", filepair.getName(), e.getMessage(), e.getCause().getMessage(), e);
            incrementErrorMetrics(e);
            return Optional.empty();
        }
    }

    private boolean lastOppFilpar(Filepair filepair, String zipName) {
        log.warn("Skanmotovrig laster opp filpar til feilområde, fil={} zipfil={}", filepair.getName(), zipName);
        String path = Utils.removeFileExtensionInFilename(zipName);
        boolean pdfOk = lastOppFil(filepair.getPdf(), filepair.getName() + ".pdf", path, zipName);
        boolean xmlOk = lastOppFil(filepair.getXml(), filepair.getName() + ".xml", path, zipName);
        return pdfOk || xmlOk;
    }

    private boolean lastOppFil(byte[] file, String name, String path, String zipName) {
        try {
            filomraadeService.uploadFileToFeilomrade(file, name, path);
            return true;
        } catch (Exception e) {
            log.error("Skanmotutgaaende feilet ved opplasting til feilområde fil={} zipFil={} feilmelding={}", name, zipName, e.getMessage(), e);
            incrementErrorMetrics(e);
            return false;
        }
    }

    private void setUpMDCforZip(String zipname) {
        MDCGenerate.setZipId(zipname);
    }

    private void tearDownMDCforZip() {
        MDCGenerate.clearZipId();
    }

    private void setUpMDCforFile(String filename) {
        MDCGenerate.setFileName(filename);
        MDCGenerate.generateNewCallIdIfThereAreNone();
    }

    private void tearDownMDCforFile() {
        MDCGenerate.clearFilename();
        MDCGenerate.clearCallId();
    }

    private void incrementMetadataMetrics(Skanningmetadata skanningmetadata) {
        dokCounter.incrementCounter(Map.of(
                TEMA, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getJournalpost)
                        .map(Journalpost::getTema)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY),
                STREKKODEPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getStrekkodePostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY),
                FYSISKPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getFysiskPostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));
    }

    private void incrementErrorMetrics(Throwable e) {
        dokCounter.incrementError(e, DokCounter.OVRIG);
    }
}
