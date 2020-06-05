package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.helse.domain.Bruker;
import no.nav.skanmotovrig.helse.domain.Journalpost;
import no.nav.skanmotovrig.helse.domain.Skanninginfo;
import no.nav.skanmotovrig.helse.domain.Skanningmetadata;
import no.nav.skanmotovrig.lagrefildetaljer.data.Dokument;
import no.nav.skanmotovrig.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.skanmotovrig.helse.OpprettJournalpostPostboksHelseRequestMapper.DOKUMENTKATEGORI;
import static no.nav.skanmotovrig.helse.OpprettJournalpostPostboksHelseRequestMapper.FNR;
import static no.nav.skanmotovrig.helse.OpprettJournalpostPostboksHelseRequestMapper.JOURNALPOSTTYPE;
import static no.nav.skanmotovrig.helse.PostboksHelseTema.PostboksHelse.PB_1411;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class OpprettJournalpostPostboksHelseTemaRequestMapperTest {
    public static final String FILEBASENAME = "pb1411";
    public static final String ZIPNAME = "pb1411.zip";
    public static final String FYSISK_POSTBOKS = "f-1411";
    public static final String STREKKODE_POSTBOKS = "1411";
    public static final byte[] XML_FIL = "pb1411.xml".getBytes();
    public static final byte[] PDF_FIL = "pb1411.pdf".getBytes();
    public static final byte[] OCR_FIL = "pb1411.ocr".getBytes();
    public static final String BATCH_NAVN = "pb1411-2020";
    public static final String ANTALL_SIDER = "1";
    public static final String MOTTAKSKANAL = "SKAN_IM";
    public static final String ENDORSERNR = "12345";
    public static final String BRUKER_ID = "***gammelt_fnr***";
    public static final String BRUKER_TYPE = "PERSON";
    public static final String TEMA_SYM = "SYM";
    public static final LocalDate DATO_MOTTATT = LocalDate.parse("2020-04-06");
    private final OpprettJournalpostPostboksHelseRequestMapper mapper = new OpprettJournalpostPostboksHelseRequestMapper();

    @Test
    void shouldMap() {
        final OpprettJournalpostRequest request = mapper.mapRequest(createEnvelope());
        assertThat(request.getJournalpostType()).isEqualTo(JOURNALPOSTTYPE);
        assertThat(request.getBruker().getId()).isEqualTo(BRUKER_ID);
        assertThat(request.getBruker().getIdType()).isEqualTo(FNR);
        assertThat(request.getTema()).isEqualTo(PB_1411.getTema());
        assertThat(request.getBehandlingstema()).isEqualTo(PB_1411.getBehandlingstema());
        assertThat(request.getTittel()).isEqualTo(PB_1411.getTittel());
        assertThat(request.getKanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(request.getEksternReferanseId()).isEqualTo(FILEBASENAME + ".pdf");
        assertThat(request.getDatoMottatt()).isEqualTo(DATO_MOTTATT.toString());
        assertThat(request.getTilleggsopplysninger()).extracting(Tilleggsopplysning::getNokkel, Tilleggsopplysning::getVerdi)
                .contains(tuple(OpprettJournalpostPostboksHelseRequestMapper.ENDORSERNR, ENDORSERNR),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.FYSISKPOSTBOKS, FYSISK_POSTBOKS),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.STREKKODEPOSTBOKS, STREKKODE_POSTBOKS),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.BATCHNAVN, BATCH_NAVN),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.ANTALL_SIDER, ANTALL_SIDER));
        assertThat(request.getDokumenter()).hasSize(1);
        final Dokument hoveddokument = request.getDokumenter().get(0);
        assertThat(hoveddokument.getBrevkode()).isEqualTo(PB_1411.getBrevkode());
        assertThat(hoveddokument.getTittel()).isEqualTo(PB_1411.getDokumentTittel());
        assertThat(hoveddokument.getDokumentKategori()).isEqualTo(DOKUMENTKATEGORI);
        assertThat(hoveddokument.getDokumentVarianter())
                .extracting(DokumentVariant::getFiltype, DokumentVariant::getVariantformat, DokumentVariant::getFysiskDokument, DokumentVariant::getFilnavn)
                .contains(tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_PDF, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_PDF, PDF_FIL, FILEBASENAME + ".pdf"),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_XML, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_XML, XML_FIL, FILEBASENAME + ".xml"),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_OCR, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_OCR, OCR_FIL, FILEBASENAME + ".ocr"));
    }

    private HelseforsendelseEnvelope createEnvelope() {
        return HelseforsendelseEnvelope.builder()
                .filebasename(FILEBASENAME)
                .zipname(ZIPNAME)
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(Journalpost.builder()
                                .bruker(Bruker.builder()
                                        .brukerId(BRUKER_ID)
                                        .brukerType(BRUKER_TYPE)
                                        .build())
                                .mottakskanal(MOTTAKSKANAL)
                                .datoMottatt(DATO_MOTTATT)
                                .batchnavn(BATCH_NAVN)
                                .endorsernr(ENDORSERNR)
                                .antallSider(ANTALL_SIDER)
                                .build())
                        .skanninginfo(Skanninginfo.builder()
                                .fysiskPostboks(FYSISK_POSTBOKS)
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .xml(XML_FIL)
                .pdf(PDF_FIL)
                .ocr(OCR_FIL)
                .build();
    }
}