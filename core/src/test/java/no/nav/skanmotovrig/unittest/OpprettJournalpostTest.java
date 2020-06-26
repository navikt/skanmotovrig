package no.nav.skanmotovrig.unittest;

import no.nav.skanmotovrig.domain.Bruker;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostRequestMapper.generateRequestBody;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OpprettJournalpostTest {

    private final String MOTTAKSKANAL = "SKAN_IM";
    private final String BATCHNAVN = "navnPaaBatch.zip";
    private final String FILNAVN_I_XML = "ABC.PDF";
    private final String FILNAVN = "filnavn";
    private final String FILNAVN_PDF = "filnavn.pdf";
    private final String FILNAVN_XML = "filnavn.xml";
    private final String ENDORSERNR = "222111NAV456";
    private final String FYSISK_POSTBOKS = "1002";
    private final String STREKKODE_POSTBOKS = "1004";
    private final String TEMA = "mockTema";
    private final String BREVKODE = "mockBrevKode";
    private final String JOURNALFOERENDEENHET = "mockJournalfoerendeEnhet";
    private final String LAND = "MockLand";
    private final byte[] DUMMY_FILE = "dummyfile".getBytes();
    private final String BRUKER_ID = "***gammelt_fnr***";
    private final String BRUKER_IDTYPE = "PERSON";
    private final String REQUEST_IDTYPE = "FNR";

    @Test
    public void shouldExtractOpprettJournalpostRequestFromSkanningmetadata() {
        OpprettJournalpostRequest opprettJournalpostRequest = generateRequestBody(
            Skanningmetadata.builder()
                    .journalpost(
                            Journalpost.builder()
                                    .tema(TEMA)
                                    .brevkode(BREVKODE)
                                    .journalfoerendeEnhet(JOURNALFOERENDEENHET)
                                    .datoMottatt(new Date())
                                    .land(LAND)
                                    .mottakskanal(MOTTAKSKANAL)
                                    .batchNavn(BATCHNAVN)
                                    .filNavn(FILNAVN_I_XML)
                                    .endorsernr(ENDORSERNR)
                                    .bruker(Bruker.builder().brukerId(BRUKER_ID).brukerType(BRUKER_IDTYPE).build())
                                    .build()
                    )
                    .skanningInfo(SkanningInfo.builder()
                            .fysiskPostboks(FYSISK_POSTBOKS)
                            .strekkodePostboks(STREKKODE_POSTBOKS)
                            .build())
                    .build(),
            Filepair.builder()
                    .name(FILNAVN)
                    .pdf(DUMMY_FILE)
                    .xml(DUMMY_FILE)
                    .build()

        );
        assertEquals(ENDORSERNR, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "endorsernr"));
        assertEquals(FYSISK_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "fysiskPostboks"));
        assertEquals(STREKKODE_POSTBOKS, getTillegsopplysningerVerdiFromNokkel(opprettJournalpostRequest.getTilleggsopplysninger(), "strekkodePostboks"));

        assertEquals(TEMA, opprettJournalpostRequest.getTema());
        assertEquals(JOURNALFOERENDEENHET, opprettJournalpostRequest.getJournalfoerendeEnhet());
        assertEquals(LAND, opprettJournalpostRequest.getAvsenderMottaker().getLand());
        assertEquals(MOTTAKSKANAL, opprettJournalpostRequest.getKanal());

        assertEquals(1, opprettJournalpostRequest.getDokumenter().size());
        assertEquals(BREVKODE, opprettJournalpostRequest.getDokumenter().iterator().next().getBrevkode());

        assertEquals(BRUKER_ID, opprettJournalpostRequest.getBruker().getId());
        assertEquals(REQUEST_IDTYPE, opprettJournalpostRequest.getBruker().getIdType());

        AtomicInteger pdfCounter = new AtomicInteger();
        AtomicInteger xmlCounter = new AtomicInteger();
        List<DokumentVariant> dokumentVarianter = opprettJournalpostRequest
                .getDokumenter()
                .iterator()
                .next()
                .getDokumentVarianter();

        dokumentVarianter.forEach(dokumentVariant -> {
            switch (dokumentVariant.getFiltype()) {
                case "PDFA":
                    pdfCounter.getAndIncrement();
                    assertEquals(FILNAVN_PDF, dokumentVariant.getFilnavn());
                    assertEquals("ARKIV", dokumentVariant.getVariantformat());
                    assertEquals(BATCHNAVN, dokumentVariant.getBatchnavn());
                    assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
                    break;
                case "XML":
                    xmlCounter.getAndIncrement();
                    assertEquals(FILNAVN_XML, dokumentVariant.getFilnavn());
                    assertEquals("SKANNING_META", dokumentVariant.getVariantformat());
                    assertEquals(BATCHNAVN, dokumentVariant.getBatchnavn());
                    assertArrayEquals(DUMMY_FILE, dokumentVariant.getFysiskDokument());
                    break;
                default:
                    fail();
            }
        });
        assertEquals(1, pdfCounter.get());
        assertEquals(1, xmlCounter.get());
    }

    private String getTillegsopplysningerVerdiFromNokkel(List<Tilleggsopplysning> tilleggsopplysninger, String nokkel) {
        return tilleggsopplysninger.stream().filter(pair -> nokkel.equals(pair.getNokkel())).findFirst().get().getVerdi();
    }

}
