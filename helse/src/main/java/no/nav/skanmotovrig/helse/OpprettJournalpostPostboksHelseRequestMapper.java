package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigFunctionalException;
import no.nav.skanmotovrig.helse.domain.Journalpost;
import no.nav.skanmotovrig.helse.domain.Skanninginfo;
import no.nav.skanmotovrig.helse.domain.Skanningmetadata;
import no.nav.skanmotovrig.lagrefildetaljer.data.Bruker;
import no.nav.skanmotovrig.lagrefildetaljer.data.Dokument;
import no.nav.skanmotovrig.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OpprettJournalpostPostboksHelseRequestMapper {
    static final String FILTYPE_PDF = "PDFA";
    static final String FILTYPE_XML = "XML";
    static final String FILTYPE_OCR = "XML";
    static final String VARIANTFORMAT_PDF = "ARKIV";
    static final String VARIANTFORMAT_XML = "SKANNING_META";
    static final String VARIANTFORMAT_OCR = "ORIGINAL";
    static final String FILE_EXTENSION_XML = "xml";
    static final String FILE_EXTENSION_PDF = "pdf";
    static final String FILE_EXTENSION_OCR = "ocr";
    static final String DOKUMENTKATEGORI = "IS";
    static final String JOURNALPOSTTYPE = "INNGAAENDE";
    static final String ENDORSERNR = "endorsernr";
    static final String FYSISKPOSTBOKS = "fysiskPostboks";
    static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    static final String BATCHNAVN = "batchNavn";
    static final String ANTALL_SIDER = "antallSider";
    static final String ORGANISASJON = "ORGANISASJON";

    static final String FNR = "FNR";
    static final String ORGNR = "ORGNR";
    static final String UKJENT_TEMA = "UKJ";
    static final String DATE_FORMAT = "yyyy-MM-dd";

    public OpprettJournalpostRequest mapRequest(PostboksHelseforsendelseEnvelope envelope) {
        final String strekkodePostboks = envelope.getSkanningmetadata().getSkanninginfo().getStrekkodePostboks();
        final PostboksHelseTema.PostboksHelse postboks = PostboksHelseTema.lookup(strekkodePostboks);
        if (postboks == null) {
            throw new SkanmotovrigFunctionalException("Fant ikke postboks metadata for strekkodePostboks=" + strekkodePostboks);
        }
        return doMap(envelope, postboks);
    }

    private OpprettJournalpostRequest doMap(PostboksHelseforsendelseEnvelope envelope, PostboksHelseTema.PostboksHelse postboks) {
        final Skanningmetadata skanningmetadata = envelope.getSkanningmetadata();
        Journalpost journalpost = skanningmetadata.getJournalpost();
        Skanninginfo skanningInfo = skanningmetadata.getSkanninginfo();
        String eksternReferanseId = envelope.createEntryName(FILE_EXTENSION_PDF);

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(FILTYPE_PDF)
                .variantformat(VARIANTFORMAT_PDF)
                .fysiskDokument(envelope.getPdf())
                .filnavn(envelope.createEntryName(FILE_EXTENSION_PDF))
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(FILTYPE_XML)
                .variantformat(VARIANTFORMAT_XML)
                .fysiskDokument(envelope.getXml())
                .filnavn(envelope.createEntryName(FILE_EXTENSION_XML))
                .build();

        Dokument dokument = createDokument(envelope, postboks, pdf, xml);

        Bruker bruker = Optional.ofNullable(journalpost.getBruker())
                .filter(jpBruker -> notNullOrEmpty(jpBruker.getBrukerType()))
                .filter(jpBruker -> notNullOrEmpty(jpBruker.getBrukerId()))
                .map(jpBruker -> Bruker.builder()
                        .idType(jpBruker.getBrukerType().equalsIgnoreCase(ORGANISASJON) ? ORGNR : FNR)
                        .id(jpBruker.getBrukerId())
                        .build()
                ).orElse(null);

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(ENDORSERNR, journalpost.getEndorsernr()),
                new Tilleggsopplysning(FYSISKPOSTBOKS, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODEPOSTBOKS, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(BATCHNAVN, journalpost.getBatchnavn()),
                new Tilleggsopplysning(ANTALL_SIDER, journalpost.getAntallSider())
        ).stream().filter(tilleggsopplysning -> notNullOrEmpty(tilleggsopplysning.getVerdi())).collect(Collectors.toList());

        String datoMottatt = journalpost.getDatoMottatt() == null
                ? null
                : journalpost.getDatoMottatt().toString();

        String tema = notNullOrEmpty(postboks.getTema()) ? postboks.getTema() : UKJENT_TEMA;

        return OpprettJournalpostRequest.builder()
                .journalpostType(JOURNALPOSTTYPE)
                .tittel(postboks.getTittel())
                .kanal(journalpost.getMottakskanal())
                .datoMottatt(datoMottatt)
                .tema(tema)
                .eksternReferanseId(eksternReferanseId)
                .tilleggsopplysninger(tilleggsopplysninger)
                .bruker(bruker)
                .dokumenter(List.of(dokument))
                .build();
    }

    private Dokument createDokument(final PostboksHelseforsendelseEnvelope envelope, final PostboksHelseTema.PostboksHelse postboks,
                                    final DokumentVariant pdf, final DokumentVariant xml) {
        if (envelope.getOcr() == null) {
            return Dokument.builder()
                    .brevkode(postboks.getBrevkode())
                    .tittel(postboks.getDokumentTittel())
                    .dokumentKategori(DOKUMENTKATEGORI)
                    .dokumentVarianter(List.of(pdf, xml))
                    .build();
        } else {
            DokumentVariant ocr = DokumentVariant.builder()
                    .filtype(FILTYPE_OCR)
                    .variantformat(VARIANTFORMAT_OCR)
                    .fysiskDokument(envelope.getOcr())
                    .filnavn(envelope.createEntryName(FILE_EXTENSION_OCR))
                    .build();
            return Dokument.builder()
                    .brevkode(postboks.getBrevkode())
                    .tittel(postboks.getDokumentTittel())
                    .dokumentKategori(DOKUMENTKATEGORI)
                    .dokumentVarianter(List.of(pdf, xml, ocr))
                    .build();
        }
    }

    private static boolean notNullOrEmpty(String string) {
        return string != null && !string.isBlank();
    }

}
