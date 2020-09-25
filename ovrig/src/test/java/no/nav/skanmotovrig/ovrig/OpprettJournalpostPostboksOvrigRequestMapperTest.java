package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.lagrefildetaljer.data.Dokument;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import no.nav.skanmotovrig.ovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.ovrig.domain.Skanningmetadata;
import org.junit.jupiter.api.Test;

import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.DOKUMENTKATEGORI;
import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.FNR;
import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.JOURNALPOSTTYPE;
import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.UKJENT_BREVKODE;
import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.UKJENT_JOURNALFOERENDE_ENHET;
import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.VARIANTFORMAT_PDF;
import static no.nav.skanmotovrig.ovrig.OpprettJournalpostPostboksOvrigRequestMapper.VARIANTFORMAT_XML;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.ANTALL_SIDER;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.BATCH_NAVN;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.BREVKODE;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.BRUKER_ID;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.DATO_MOTTATT;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.ENDORSERNR;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.FILEBASENAME;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.FYSISK_POSTBOKS;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.JOURNALFORENDEENHET;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.LAND;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.MOTTAKSKANAL;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.PDF_FIL;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.STREKKODE_POSTBOKS;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.SYVSIFRET_BREVKODE;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.TEMA;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.UKJENT_TEMA;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.XML_FIL;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.ZIPNAME;
import static no.nav.skanmotovrig.ovrig.PostboksOvrigEnvelopeTestObjects.createFullJournalpost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class OpprettJournalpostPostboksOvrigRequestMapperTest {
    private final OpprettJournalpostPostboksOvrigRequestMapper mapper = new OpprettJournalpostPostboksOvrigRequestMapper();


    @Test
    void shouldMapMinimalEnvelope() {
        final PostboksOvrigEnvelope envelope = PostboksOvrigEnvelopeTestObjects.createMinimalEnvelope().build();
        final OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        assertThat(request.getBruker()).isEqualTo(null);
        assertThat(request.getTittel()).isEqualTo(null);
        assertThat(request.getAvsenderMottaker()).isEqualTo(null);
        assertThat(request.getJournalpostType()).isEqualTo(JOURNALPOSTTYPE);
        assertThat(request.getTema()).isEqualTo(UKJENT_TEMA);
        assertThat(request.getBehandlingstema()).isEqualTo(null);
        assertThat(request.getKanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(request.getDatoMottatt()).isEqualTo(DATO_MOTTATT.toString());
        assertThat(request.getJournalfoerendeEnhet()).isEqualTo(null);
        assertThat(request.getEksternReferanseId()).isEqualTo(FILEBASENAME + ".pdf");
        assertThat(request.getTilleggsopplysninger()).extracting(Tilleggsopplysning::getNokkel, Tilleggsopplysning::getVerdi)
                .containsExactly(tuple(OpprettJournalpostPostboksOvrigRequestMapper.STREKKODEPOSTBOKS, STREKKODE_POSTBOKS));
        assertThat(request.getDokumenter().size()).isEqualTo(1);

        final Dokument dokument = request.getDokumenter().get(0);
        assertThat(dokument.getBrevkode()).isEqualTo(null);
        assertThat(dokument.getDokumentKategori()).isEqualTo(DOKUMENTKATEGORI);
        assertThat(dokument.getTittel()).isEqualTo(null);
        assertThat(dokument.getDokumentVarianter().size()).isEqualTo(2);

        assertThat(dokument.getDokumentVarianter()).anySatisfy(dokumentVariant -> {
            assertThat(dokumentVariant.getFiltype()).isEqualTo("PDFA");
            assertThat(dokumentVariant.getBatchnavn()).isEqualTo(BATCH_NAVN);
            assertThat(dokumentVariant.getFysiskDokument()).isEqualTo(PDF_FIL);
            assertThat(dokumentVariant.getVariantformat()).isEqualTo(VARIANTFORMAT_PDF);
            assertThat(dokumentVariant.getFilnavn()).isEqualTo(FILEBASENAME + ".pdf");
        });
        assertThat(dokument.getDokumentVarianter()).anySatisfy(dokumentVariant -> {
            assertThat(dokumentVariant.getFiltype()).isEqualTo("XML");
            assertThat(dokumentVariant.getBatchnavn()).isEqualTo(BATCH_NAVN);
            assertThat(dokumentVariant.getFysiskDokument()).isEqualTo(XML_FIL);
            assertThat(dokumentVariant.getVariantformat()).isEqualTo(VARIANTFORMAT_XML);
            assertThat(dokumentVariant.getFilnavn()).isEqualTo(FILEBASENAME + ".xml");
        });
    }

    @Test
    void shouldMapFullEnvelope() {
        final PostboksOvrigEnvelope envelope = PostboksOvrigEnvelopeTestObjects.createFullEnvelope().build();
        final OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        assertThat(request.getBruker().getIdType()).isEqualTo(FNR);
        assertThat(request.getBruker().getId()).isEqualTo(BRUKER_ID);
        assertThat(request.getTittel()).isEqualTo(null);
        assertThat(request.getAvsenderMottaker().getLand()).isEqualTo(LAND);
        assertThat(request.getJournalpostType()).isEqualTo(JOURNALPOSTTYPE);
        assertThat(request.getTema()).isEqualTo(TEMA);
        assertThat(request.getBehandlingstema()).isEqualTo(null);
        assertThat(request.getKanal()).isEqualTo(MOTTAKSKANAL);
        assertThat(request.getDatoMottatt()).isEqualTo(DATO_MOTTATT.toString());
        assertThat(request.getJournalfoerendeEnhet()).isEqualTo(JOURNALFORENDEENHET);
        assertThat(request.getEksternReferanseId()).isEqualTo(FILEBASENAME + ".pdf");
        assertThat(request.getTilleggsopplysninger()).extracting(Tilleggsopplysning::getNokkel, Tilleggsopplysning::getVerdi)
                .containsExactly(
                        tuple(OpprettJournalpostPostboksOvrigRequestMapper.ENDORSERNR, ENDORSERNR),
                        tuple(OpprettJournalpostPostboksOvrigRequestMapper.FYSISKPOSTBOKS, FYSISK_POSTBOKS),
                        tuple(OpprettJournalpostPostboksOvrigRequestMapper.STREKKODEPOSTBOKS, STREKKODE_POSTBOKS),
                        tuple(OpprettJournalpostPostboksOvrigRequestMapper.ANTALL_SIDER, ANTALL_SIDER)
                );
        assertThat(request.getDokumenter().size()).isEqualTo(1);

        final Dokument dokument = request.getDokumenter().get(0);
        assertThat(dokument.getBrevkode()).isEqualTo(BREVKODE);
        assertThat(dokument.getDokumentKategori()).isEqualTo(DOKUMENTKATEGORI);
        assertThat(dokument.getTittel()).isEqualTo(null);
        assertThat(dokument.getDokumentVarianter().size()).isEqualTo(2);

        assertThat(dokument.getDokumentVarianter()).anySatisfy(dokumentVariant -> {
            assertThat(dokumentVariant.getFiltype()).isEqualTo("PDFA");
            assertThat(dokumentVariant.getBatchnavn()).isEqualTo(BATCH_NAVN);
            assertThat(dokumentVariant.getFysiskDokument()).isEqualTo(PDF_FIL);
            assertThat(dokumentVariant.getVariantformat()).isEqualTo(VARIANTFORMAT_PDF);
            assertThat(dokumentVariant.getFilnavn()).isEqualTo(FILEBASENAME + ".pdf");
        });
        assertThat(dokument.getDokumentVarianter()).anySatisfy(dokumentVariant -> {
            assertThat(dokumentVariant.getFiltype()).isEqualTo("XML");
            assertThat(dokumentVariant.getBatchnavn()).isEqualTo(BATCH_NAVN);
            assertThat(dokumentVariant.getFysiskDokument()).isEqualTo(XML_FIL);
            assertThat(dokumentVariant.getVariantformat()).isEqualTo(VARIANTFORMAT_XML);
            assertThat(dokumentVariant.getFilnavn()).isEqualTo(FILEBASENAME + ".xml");
        });
    }

    @Test
    void shouldMapJournalfoerendeEnhetAsNullWhen0000() {
        PostboksOvrigEnvelope envelope = PostboksOvrigEnvelope.builder()
                .filebasename(FILEBASENAME)
                .zipname(ZIPNAME)
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(createFullJournalpost()
                                .journalforendeEnhet(UKJENT_JOURNALFOERENDE_ENHET)
                                .build())
                        .skanningInfo(SkanningInfo.builder()
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .xml(XML_FIL)
                .pdf(PDF_FIL)
                .build();
        final OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        assertThat(request.getJournalfoerendeEnhet()).isEqualTo(null);
    }

    @Test
    void shouldMapBrevkodeAsNullWhen000000() {
        PostboksOvrigEnvelope envelope = PostboksOvrigEnvelope.builder()
                .filebasename(FILEBASENAME)
                .zipname(ZIPNAME)
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(createFullJournalpost()
                                .brevKode(UKJENT_BREVKODE)
                                .build())
                        .skanningInfo(SkanningInfo.builder()
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .xml(XML_FIL)
                .pdf(PDF_FIL)
                .build();
        final OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        final Dokument hoveddokument = request.getDokumenter().get(0);
        assertThat(hoveddokument.getBrevkode()).isNull();
    }

    @Test
    void shouldMapBrevkodeAsNullWhen7Digits() {
        PostboksOvrigEnvelope envelope = PostboksOvrigEnvelope.builder()
                .filebasename(FILEBASENAME)
                .zipname(ZIPNAME)
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(createFullJournalpost()
                                .brevKode(SYVSIFRET_BREVKODE)
                                .build())
                        .skanningInfo(SkanningInfo.builder()
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .xml(XML_FIL)
                .pdf(PDF_FIL)
                .build();
        final OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        final Dokument hoveddokument = request.getDokumenter().get(0);
        assertThat(hoveddokument.getBrevkode()).isNull();
    }
}