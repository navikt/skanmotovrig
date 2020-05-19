package no.nav.skanmotovrig.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.itest.config.TestConfig;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostConsumer;
import no.nav.skanmotovrig.lagrefildetaljer.STSConsumer;
import no.nav.skanmotovrig.lagrefildetaljer.data.Dokument;
import no.nav.skanmotovrig.lagrefildetaljer.data.DokumentVariant;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSResponse;
import no.nav.skanmotovrig.lagrefildetaljer.data.Tilleggsopplysning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class LagreFildetaljerIT {

    private final byte[] DUMMY_FILE = "dummyfile".getBytes();
    private final String JOURNALPOST_ID = "001";
    private final String JOURNALPOST_ID_INVALID = "002";
    private final String MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE = "/rest/journalpostapi/v1/journalpost\\?foersoekFerdigstill=false";
    private final String STSUrl = "/rest/v1/sts/token";

    private OpprettJournalpostConsumer opprettJournalpostConsumer;
    private STSConsumer stsConsumer;

    @Autowired
    private SkanmotovrigProperties skanmotovrigProperties;

    @BeforeEach
    void setUpConsumer() {
        setUpStubs();
        stsConsumer = new STSConsumer(new RestTemplateBuilder(), skanmotovrigProperties);
        opprettJournalpostConsumer = new OpprettJournalpostConsumer(new RestTemplateBuilder(), skanmotovrigProperties);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    private void setUpStubs() {
        stubFor(post(urlMatching(MOTTA_DOKUMENT_UTGAAENDE_SKANNING_TJENESTE))
            .willReturn(aResponse().withStatus(HttpStatus.OK.value())));
        stubFor(post(urlMatching(STSUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Json.node(
                        "{\"access_token\":\"MockToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
                )))
        );
    }


    @Test
    public void shouldOpprettJournalpost() {
        OpprettJournalpostRequest request = createOpprettJournalpostRequest();
        STSResponse stsResponse = stsConsumer.getSTSToken();
        OpprettJournalpostResponse res = opprettJournalpostConsumer.lagreFilDetaljer(stsResponse.getAccess_token(), request);
        assertEquals(null, res);
    }


    /*
        public static final String ENDORSER_NR = "endorsernr";
    public static final String FYSISK_POSTBOKS = "fysiskPostboks";
    public static final String STREKKODE_POSTBOKS = "strekkodePostboks";
    public static final String FILTYPE_PDFA = "PDFA";
    public static final String FILTYPE_XML = "XML";
    public static final String VARIANTFORMAT_ARKIV = "ARKIV";
    public static final String VARIANTFORMAT_SKANNING_META = "SKANNING_META";
     */

    private OpprettJournalpostRequest createOpprettJournalpostRequest() {
        List<Tilleggsopplysning> tilleggsopplysninger = List.of(
                new Tilleggsopplysning("batchNavn", "xml_pdf_pairs_testdata.zip"),
                new Tilleggsopplysning("fysiskPostboks", "1400"),
                new Tilleggsopplysning("strekkodePostboks", "1400"),
                new Tilleggsopplysning("endorsernr", "3110190003NAV743506")
        );

        DokumentVariant pdf = DokumentVariant.builder()
            .filtype("pdf")
            .variantformat("ARKIV")
            .fysiskDokument(DUMMY_FILE)
            .filnavn("dummy.pdf")
            .build();

        DokumentVariant xml = DokumentVariant.builder()
            .filtype("xml")
            .variantformat("ORIGINAL")
            .fysiskDokument(DUMMY_FILE)
            .filnavn("dummy.xml")
            .build();

        List<Dokument> dokumenter = List.of(
                Dokument.builder()
                        .dokumentVarianter(List.of(pdf, xml))
                        .build()
        );

        return OpprettJournalpostRequest.builder()
                .tilleggsopplysninger(tilleggsopplysninger)
                .dokumenter(dokumenter)
                .build();
    }
}
