package no.nav.skanmotovrig.lesoglagre;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.exceptions.functional.InvalidMetadataException;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigUnzipperFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.AbstractSkanmotovrigTechnicalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.leszipfil.LesZipfilService;
import no.nav.skanmotovrig.unzipskanningmetadata.UnzipSkanningmetadataUtils;
import no.nav.skanmotovrig.unzipskanningmetadata.Unzipper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LesFraFilomraadeOgOpprettJournalpost {

    private final LesZipfilService lesZipfilService;
    private final OpprettJournalpostService opprettJournalpostService;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public LesFraFilomraadeOgOpprettJournalpost(LesZipfilService lesZipfilService,
                                                OpprettJournalpostService opprettJournalpostService) {
        this.lesZipfilService = lesZipfilService;
        this.opprettJournalpostService = opprettJournalpostService;
    }

    @Scheduled(initialDelay = 3000, fixedDelay = 72 * HOUR)
    public void scheduledJob() {
        lesOgLagre();
    }

    public List<List<OpprettJournalpostResponse>> lesOgLagre() {

        Map<String, byte[]> zipfiles = lesFil();

        log.info("Skanmotutgaaende leste fra filområde og fant {} zipfiler", zipfiles.size());

        List<List<OpprettJournalpostResponse>> allResponses = new ArrayList<>();
        for (String zipname : zipfiles.keySet()) {
            try {
                List<Filepair> filepairList = Unzipper.unzipXmlPdf(zipfiles.get(zipname));

                List<OpprettJournalpostResponse> responses = filepairList.stream()
                        .map(this::lagreFil)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                log.info("Skanmotutgaaende lagret fildetaljer fra zipfil {} i dokarkiv", zipname);
                allResponses.add(responses);
            } catch (IOException e) {
                // TODO: Håndter denne feilen skikkelig. Løses av MMA-4346
                log.error("Skanmotutgaaende klarte ikke lese fra fil {}", zipname, e);
            } catch (SkanmotovrigUnzipperFunctionalException e) {
                // TODO: Håndter denne feilen skikkelig. Løses av MMA-4346
                log.error("Skanmotutgaaende feilet i unzipping av fil {}", zipname, e);
            }
        }
        return allResponses;
    }

    private Map<String, byte[]> lesFil() {
        try {
            return lesZipfilService.getZipFiles();
        } catch (SkanmotovrigSftpTechnicalException e) {
            return new HashMap<>();
        }
    }

    private OpprettJournalpostResponse lagreFil(Filepair filepair) {
        OpprettJournalpostResponse response = null;
        FilepairWithMetadata filepairWithMetadata = extractMetadata(filepair);
        if (filepairWithMetadata == null) {
            return null;
        }
        try {
            response = opprettJournalpostService.lagreFildetaljer(filepairWithMetadata);
            log.info("Skanmotutgaaende lagret fildetaljer for journalpost");
        } catch (AbstractSkanmotovrigFunctionalException e) {
            // TODO: Feilhåndtering
            log.error("Skanmotutgaaende feilet funskjonelt med lagring av fildetaljer til journalpost", e);
        } catch (AbstractSkanmotovrigTechnicalException e) {
            // TODO: Feilhåndtering
            log.error("Skanmotutgaaende feilet teknisk med lagring av fildetaljer til journalpost", e);
        }
        return response;
    }

    private FilepairWithMetadata extractMetadata(Filepair filepair) {
        try {
            return UnzipSkanningmetadataUtils.extractMetadata(filepair);
        } catch (InvalidMetadataException e) {
            log.warn("Skanningmetadata hadde ugyldige verdier for fil {}", filepair.getName(), e);
            return null;
        }
    }
}
