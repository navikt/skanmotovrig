package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.lagrefildetaljer.data.AvsenderMottaker;
import no.nav.skanmotovrig.lagrefildetaljer.data.Bruker;
import no.nav.skanmotovrig.lagrefildetaljer.data.Dokument;
import no.nav.skanmotovrig.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import no.nav.skanmotovrig.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpprettJournalpostRequestMapper {

    private static final String PDFA = "PDFA";
    private static final String XML = "XML";
    private static final String VARIANTFORMAT_PDF = "ARKIV";
    private static final String VARIANTFORMAT_XML = "SKANNING_META";
    private static final String FILTYPE_XML = "xml";
    private static final String DOKUMENTKATEGORI = "IS";
    private static final String JOURNALPOSTTYPE = "INNGAAENDE";
    private static final String ENDORSERNR = "endorsernr";
    private static final String FYSISKPOSTBOKS = "fysiskPostboks";
    private static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    private static final String BATCHNAVN = "batchNavn";
    private static final String ORGANISASJON = "ORGANISASJON";

    private static final String FNR = "FNR";
    private static final String ORGNR = "ORGNR";
    private static final String UKJENT_TEMA = "UKJ";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static OpprettJournalpostRequest generateRequestBody(Skanningmetadata skanningmetadata, Filepair filepair) {
        Journalpost journalpost = skanningmetadata.getJournalpost();
        SkanningInfo skanningInfo = skanningmetadata.getSkanningInfo();

        AvsenderMottaker avsenderMottaker = null;
        if(journalpost.getLand() != null) {
            avsenderMottaker = AvsenderMottaker.builder()
                    .land(journalpost.getLand())
                    .build();
        }

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(PDFA)
                .variantformat(VARIANTFORMAT_PDF)
                .fysiskDokument(filepair.getPdf())
                .filnavn(journalpost.getFilNavn())
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(XML)
                .variantformat(VARIANTFORMAT_XML)
                .fysiskDokument(filepair.getXml())
                .filnavn(Utils.changeFiletypeInFilename(journalpost.getFilNavn(), FILTYPE_XML))
                .build();

        Dokument dokument = Dokument.builder()
                .brevkode(journalpost.getBrevkode())
                .dokumentKategori(DOKUMENTKATEGORI)
                .dokumentVarianter(List.of(pdf, xml))
                .build();

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
                new Tilleggsopplysning(BATCHNAVN, journalpost.getBatchNavn())
        ).stream().filter(tilleggsopplysning -> notNullOrEmpty(tilleggsopplysning.getVerdi())).collect(Collectors.toList());

        String datoMottatt = journalpost.getDatoMottatt() == null
                ? null
                : new SimpleDateFormat(DATE_FORMAT).format(journalpost.getDatoMottatt());

        String tema = notNullOrEmpty(journalpost.getTema())? journalpost.getTema() : UKJENT_TEMA;

        return OpprettJournalpostRequest.builder()
                .journalfoerendeEnhet(journalpost.getJournalfoerendeEnhet())
                .journalpostType(JOURNALPOSTTYPE)
                .avsenderMottaker(avsenderMottaker)
                .kanal(journalpost.getMottakskanal())
                .datoMottatt(datoMottatt)
                .tema(tema)
                .eksternReferanseId(journalpost.getFilNavn())
                .tilleggsopplysninger(tilleggsopplysninger)
                .bruker(bruker)
                .dokumenter(List.of(dokument))
                .build();
    }

    private static boolean notNullOrEmpty(String string) {
        return string != null && !string.isBlank();
    }
}
