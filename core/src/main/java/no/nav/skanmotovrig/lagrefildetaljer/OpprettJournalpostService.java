package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.domain.Journalpost;
import no.nav.skanmotovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.lagrefildetaljer.data.AvsenderMottaker;
import no.nav.skanmotovrig.lagrefildetaljer.data.Bruker;
import no.nav.skanmotovrig.lagrefildetaljer.data.Dokument;
import no.nav.skanmotovrig.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSResponse;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import no.nav.skanmotovrig.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OpprettJournalpostService {

    private OpprettJournalpostConsumer opprettJournalpostConsumer;
    private STSConsumer stsConsumer;

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

    private static final String PERSON = "PERSON";
    private static final String ORGANISASJON = "ORGANISASJON";

    private static final String FNR = "FNR";
    private static final String ORGNR = "ORGNR";


    @Inject
    public OpprettJournalpostService(OpprettJournalpostConsumer opprettJournalpostConsumer, STSConsumer stsConsumer) {
        this.opprettJournalpostConsumer = opprettJournalpostConsumer;
        this.stsConsumer = stsConsumer;
    }

    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        STSResponse stsResponse = stsConsumer.getSTSToken();
        return opprettJournalpostConsumer.lagreFilDetaljer(stsResponse.getAccess_token(), request);
    }

    public OpprettJournalpostResponse opprettJournalpost(FilepairWithMetadata filepairWithMetadata) {
        OpprettJournalpostRequest request = extractLagreFildetaljerRequestFromSkanningmetadata(filepairWithMetadata);
        return opprettJournalpost(request);
    }

    public static OpprettJournalpostRequest extractLagreFildetaljerRequestFromSkanningmetadata(FilepairWithMetadata filepairWithMetadata) {
        Journalpost journalpost = filepairWithMetadata.getSkanningmetadata().getJournalpost();
        SkanningInfo skanningInfo = filepairWithMetadata.getSkanningmetadata().getSkanningInfo();

        AvsenderMottaker avsenderMottaker = null;
        if(journalpost.getLand() != null) {
            avsenderMottaker = AvsenderMottaker.builder()
                    .land(journalpost.getLand())
                    .build();
        }

        DokumentVariant pdf = DokumentVariant.builder()
                .filtype(PDFA)
                .variantformat(VARIANTFORMAT_PDF)
                .fysiskDokument(filepairWithMetadata.getPdf())
                .filnavn(journalpost.getFilNavn())
                .build();

        DokumentVariant xml = DokumentVariant.builder()
                .filtype(XML)
                .variantformat(VARIANTFORMAT_XML)
                .fysiskDokument(filepairWithMetadata.getXml())
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
                        .idType(jpBruker.getBrukerType().equalsIgnoreCase(PERSON) ? FNR : ORGNR)
                        .id(jpBruker.getBrukerId())
                        .build()
                ).orElse(null);

        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning(ENDORSERNR, journalpost.getEndorsernr()),
                new Tilleggsopplysning(FYSISKPOSTBOKS, skanningInfo.getFysiskPostboks()),
                new Tilleggsopplysning(STREKKODEPOSTBOKS, skanningInfo.getStrekkodePostboks()),
                new Tilleggsopplysning(BATCHNAVN, journalpost.getBatchNavn())
        ).stream().filter(tilleggsopplysning -> tilleggsopplysning.getVerdi() != null).collect(Collectors.toList());


        return OpprettJournalpostRequest.builder()
                .journalfoerendeEnhet(journalpost.getJournalfoerendeEnhet())
                .journalpostType(JOURNALPOSTTYPE)
                .avsenderMottaker(avsenderMottaker)
                .kanal(journalpost.getMottakskanal())
                .tema(journalpost.getTema())
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
