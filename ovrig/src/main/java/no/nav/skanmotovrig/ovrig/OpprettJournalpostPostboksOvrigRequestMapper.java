package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.lagrefildetaljer.data.AvsenderMottaker;
import no.nav.skanmotovrig.ovrig.domain.Journalpost;
import no.nav.skanmotovrig.ovrig.domain.SkanningInfo;
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
public class OpprettJournalpostPostboksOvrigRequestMapper {
    static final String FILTYPE_PDF = "PDFA";
    static final String FILTYPE_XML = "XML";
    static final String VARIANTFORMAT_PDF = "ARKIV";
    static final String VARIANTFORMAT_XML = "SKANNING_META";
    static final String FILE_EXTENSION_XML = "xml";
    static final String FILE_EXTENSION_PDF = "pdf";
    static final String DOKUMENTKATEGORI = "IS";
    static final String JOURNALPOSTTYPE = "INNGAAENDE";
    static final String ENDORSERNR = "endorsernr";
    static final String FYSISKPOSTBOKS = "fysiskPostboks";
    static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    static final String ANTALL_SIDER = "antallSider";
    static final String ORGANISASJON = "ORGANISASJON";

    static final String FNR = "FNR";
    static final String ORGNR = "ORGNR";
    static final String UKJENT_TEMA = "UKJ";

    public OpprettJournalpostRequest mapRequest(PostboksOvrigEnvelope envelope){
        Journalpost journalpost = envelope.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = envelope.getSkanningmetadata().getSkanningInfo();
        String eksternReferanseId = appendFileType(envelope.getFilebasename(), FILE_EXTENSION_PDF);
        String batchnavn = journalpost.getBatchnavn();

        String tema = mapToValueIfEmpty(journalpost.getTema(), UKJENT_TEMA);
        String brevKode = mapToValueIfEmpty(journalpost.getBrevKode(), null);
        String journalforendeEnhet = mapToValueIfEmpty(journalpost.getJournalforendeEnhet(), null);
        String land = mapToValueIfEmpty(journalpost.getLand(), null);

        AvsenderMottaker avsenderMottaker = (land == null) ? null : AvsenderMottaker.builder().land(land).build();

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(FILTYPE_PDF)
                .variantformat(VARIANTFORMAT_PDF)
                .fysiskDokument(envelope.getPdf())
                .filnavn(appendFileType(envelope.getFilebasename(), FILE_EXTENSION_PDF))
                .batchnavn(batchnavn)
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(FILTYPE_XML)
                .variantformat(VARIANTFORMAT_XML)
                .fysiskDokument(envelope.getXml())
                .filnavn(appendFileType(envelope.getFilebasename(), FILE_EXTENSION_XML))
                .batchnavn(batchnavn)
                .build();

        Dokument dokument = Dokument.builder()
                .brevkode(brevKode)
                .dokumentKategori(DOKUMENTKATEGORI)
                .dokumentVarianter(List.of(pdf, xml))
                .build();

        Bruker bruker = Optional.ofNullable(journalpost.getBruker())
                .filter(jpBruker -> notNullOrEmpty(jpBruker.getBrukertype()))
                .filter(jpBruker -> notNullOrEmpty(jpBruker.getBrukerId()))
                .map(jpBruker -> Bruker.builder()
                        .idType(jpBruker.getBrukertype().equalsIgnoreCase(ORGANISASJON) ? ORGNR : FNR)
                        .id(jpBruker.getBrukerId())
                        .build()
                ).orElse(null);

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(ENDORSERNR, journalpost.getEndorsernr()),
                new Tilleggsopplysning(FYSISKPOSTBOKS, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODEPOSTBOKS, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(ANTALL_SIDER, journalpost.getAntallSider())
        ).stream().filter(tilleggsopplysning -> notNullOrEmpty(tilleggsopplysning.getVerdi())).collect(Collectors.toList());

        String datoMottatt = journalpost.getDatoMottatt() == null
                ? null
                : journalpost.getDatoMottatt().toString();


        return OpprettJournalpostRequest.builder()
                .journalfoerendeEnhet(journalforendeEnhet)
                .journalpostType(JOURNALPOSTTYPE)
                .avsenderMottaker(avsenderMottaker)
                .kanal(journalpost.getMottakskanal())
                .datoMottatt(datoMottatt)
                .tema(tema)
                .eksternReferanseId(eksternReferanseId)
                .tilleggsopplysninger(tilleggsopplysninger)
                .bruker(bruker)
                .dokumenter(List.of(dokument))
                .build();
    }

    private static String appendFileType(String filename, String filetype) {
        return filename + "." + filetype;
    }

    private static boolean notNullOrEmpty(String string) {
        return string != null && !string.isBlank();
    }

    private static String mapToValueIfEmpty(String string, String value) {
        return notNullOrEmpty(string) ? string : value;
    }
}
