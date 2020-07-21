package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.helse.domain.Bruker;
import no.nav.skanmotovrig.helse.domain.Journalpost;
import no.nav.skanmotovrig.helse.domain.Skanninginfo;
import no.nav.skanmotovrig.helse.domain.Skanningmetadata;

import java.time.LocalDate;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class PostboksHelseEnvelopeTestObjects {
    public static final String FILEBASENAME = "pb1411";
    public static final String ZIPNAME = "pb1411.zip";
    public static final String FYSISK_POSTBOKS = "f-1411";
    public static final String STREKKODE_POSTBOKS = "1411";
    public static final byte[] XML_FIL = "pb1411.xml" .getBytes();
    public static final byte[] PDF_FIL = "pb1411.pdf" .getBytes();
    public static final byte[] OCR_FIL = "pb1411.ocr" .getBytes();
    public static final String BATCH_NAVN = "pb1411-2020";
    public static final String ANTALL_SIDER = "1";
    public static final String MOTTAKSKANAL = "SKAN_IM";
    public static final String ENDORSERNR = "12345";
    public static final String BRUKER_ID = "11111111111";
    public static final String BRUKER_TYPE = "PERSON";
    public static final LocalDate DATO_MOTTATT = LocalDate.parse("2020-04-06");

    static PostboksHelseEnvelope createEnvelope() {
        return createBaseEnvelope()
                .build();
    }

    static PostboksHelseEnvelope createEnvelopeWithOcr() {
        return createBaseEnvelope()
                .ocr(OCR_FIL)
                .build();
    }

    static PostboksHelseEnvelope.PostboksHelseEnvelopeBuilder createBaseEnvelope() {
        return PostboksHelseEnvelope.builder()
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
                .pdf(PDF_FIL);
    }
}
