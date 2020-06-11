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

import static no.nav.skanmotovrig.helse.OpprettJournalpostPostboksHelseRequestMapper.DOKUMENTKATEGORI;
import static no.nav.skanmotovrig.helse.OpprettJournalpostPostboksHelseRequestMapper.FNR;
import static no.nav.skanmotovrig.helse.OpprettJournalpostPostboksHelseRequestMapper.JOURNALPOSTTYPE;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.ANTALL_SIDER;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.BATCH_NAVN;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.BRUKER_ID;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.BRUKER_TYPE;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.DATO_MOTTATT;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.ENDORSERNR;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.FYSISK_POSTBOKS;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.MOTTAKSKANAL;
import static no.nav.skanmotovrig.helse.PostboksHelseEnvelopeTestObjects.STREKKODE_POSTBOKS;
import static no.nav.skanmotovrig.helse.PostboksHelseTema.PostboksHelse.PB_1411;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class OpprettJournalpostPostboksHelseRequestMapperTest {
    private final OpprettJournalpostPostboksHelseRequestMapper mapper = new OpprettJournalpostPostboksHelseRequestMapper();

    @Test
    void shouldMap() {
        final OpprettJournalpostRequest request = mapper.mapRequest(PostboksHelseEnvelopeTestObjects.createEnvelopeWithOcr());
        assertThat(request.getJournalpostType()).isEqualTo(JOURNALPOSTTYPE);
        assertThat(request.getBruker().getId()).isEqualTo(BRUKER_ID);
        assertThat(request.getBruker().getIdType()).isEqualTo(FNR);
        assertThat(request.getTema()).isEqualTo(PB_1411.getTema());
        assertThat(request.getBehandlingstema()).isEqualTo(PB_1411.getBehandlingstema());
        assertThat(request.getTittel()).isEqualTo(PB_1411.getTittel());
        assertThat(request.getKanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(request.getEksternReferanseId()).isEqualTo(PostboksHelseEnvelopeTestObjects.FILEBASENAME + ".pdf");
        assertThat(request.getDatoMottatt()).isEqualTo(DATO_MOTTATT.toString());
        assertThat(request.getTilleggsopplysninger()).hasSize(5);
        assertThat(request.getTilleggsopplysninger()).extracting(Tilleggsopplysning::getNokkel, Tilleggsopplysning::getVerdi)
                .containsExactly(tuple(OpprettJournalpostPostboksHelseRequestMapper.ENDORSERNR, ENDORSERNR),
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
                .containsExactly(tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_PDF, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_PDF, PostboksHelseEnvelopeTestObjects.PDF_FIL, PostboksHelseEnvelopeTestObjects.FILEBASENAME + ".pdf"),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_XML, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_XML, PostboksHelseEnvelopeTestObjects.XML_FIL, PostboksHelseEnvelopeTestObjects.FILEBASENAME + ".xml"),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_OCR, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_OCR, PostboksHelseEnvelopeTestObjects.OCR_FIL, PostboksHelseEnvelopeTestObjects.FILEBASENAME + ".ocr"));
    }

    @Test
    void shouldMapOnlyPdfAndXmlWhenNoOcrFile() {
        final OpprettJournalpostRequest request = mapper.mapRequest(PostboksHelseEnvelopeTestObjects.createEnvelope());
        final Dokument hoveddokument = request.getDokumenter().get(0);
        assertThat(hoveddokument.getDokumentVarianter())
                .extracting(DokumentVariant::getFiltype, DokumentVariant::getVariantformat, DokumentVariant::getFysiskDokument, DokumentVariant::getFilnavn)
                .containsExactly(tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_PDF, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_PDF, PostboksHelseEnvelopeTestObjects.PDF_FIL, PostboksHelseEnvelopeTestObjects.FILEBASENAME + ".pdf"),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.FILTYPE_XML, OpprettJournalpostPostboksHelseRequestMapper.VARIANTFORMAT_XML, PostboksHelseEnvelopeTestObjects.XML_FIL, PostboksHelseEnvelopeTestObjects.FILEBASENAME + ".xml"));

    }

    @Test
    void shouldMapTilleggsopplysningerWithNoEndorsernrWhenValueIsNull() {
        final OpprettJournalpostRequest request = mapper.mapRequest(PostboksHelseEnvelopeTestObjects.createBaseEnvelope()
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(Journalpost.builder()
                                .bruker(Bruker.builder()
                                        .brukerId(BRUKER_ID)
                                        .brukerType(BRUKER_TYPE)
                                        .build())
                                .mottakskanal(MOTTAKSKANAL)
                                .datoMottatt(DATO_MOTTATT)
                                .batchnavn(BATCH_NAVN)
                                .endorsernr(null)
                                .antallSider(ANTALL_SIDER)
                                .build())
                        .skanninginfo(Skanninginfo.builder()
                                .fysiskPostboks(FYSISK_POSTBOKS)
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .build());
        assertThat(request.getTilleggsopplysninger()).hasSize(4);
        assertThat(request.getTilleggsopplysninger()).extracting(Tilleggsopplysning::getNokkel, Tilleggsopplysning::getVerdi)
                .containsExactly(tuple(OpprettJournalpostPostboksHelseRequestMapper.FYSISKPOSTBOKS, FYSISK_POSTBOKS),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.STREKKODEPOSTBOKS, STREKKODE_POSTBOKS),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.BATCHNAVN, BATCH_NAVN),
                        tuple(OpprettJournalpostPostboksHelseRequestMapper.ANTALL_SIDER, ANTALL_SIDER));
    }
}