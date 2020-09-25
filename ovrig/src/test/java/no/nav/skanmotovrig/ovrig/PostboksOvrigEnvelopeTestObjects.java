package no.nav.skanmotovrig.ovrig;


import no.nav.skanmotovrig.ovrig.domain.Bruker;
import no.nav.skanmotovrig.ovrig.domain.Journalpost;
import no.nav.skanmotovrig.ovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.ovrig.domain.Skanningmetadata;

import java.time.LocalDate;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class PostboksOvrigEnvelopeTestObjects {
    public static final String FILEBASENAME = "pb1406";
    public static final String ZIPNAME = "pb1406.zip";
    public static final String FYSISK_POSTBOKS = "f-1406";
    public static final String STREKKODE_POSTBOKS = "1406";
    public static final byte[] XML_FIL = "pb1406.xml".getBytes();
    public static final byte[] PDF_FIL = "pb1406.pdf".getBytes();
    public static final String BATCH_NAVN = "pb1406-2020";
    public static final String ANTALL_SIDER = "1";
    public static final String MOTTAKSKANAL = "SKAN_IM";
    public static final String ENDORSERNR = "12345";
    public static final String BRUKER_ID = "11111111111";
    public static final String BRUKER_TYPE = "PERSON";
    public static final LocalDate DATO_MOTTATT = LocalDate.parse("2020-04-06");
    public static final String TEMA = "PEN";
    public static final String UKJENT_TEMA = "UKJ";
    public static final String BREVKODE = "BREVKODE";
    public static final String SYVSIFRET_BREVKODE = "1000000";
    public static final String JOURNALFORENDEENHET = "JOURNALFORENDEENHET";
    public static final String LAND = "SWE";

    static PostboksOvrigEnvelope.PostboksOvrigEnvelopeBuilder createFullEnvelope() {
        return PostboksOvrigEnvelope.builder()
                .filebasename(FILEBASENAME)
                .zipname(ZIPNAME)
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(createFullJournalpost()
                                .build())
                        .skanningInfo(SkanningInfo.builder()
                                .fysiskPostboks(FYSISK_POSTBOKS)
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .xml(XML_FIL)
                .pdf(PDF_FIL);
    }

    static Journalpost.JournalpostBuilder createFullJournalpost() {
        return Journalpost.builder()
                .bruker(Bruker.builder()
                        .brukerId(BRUKER_ID)
                        .brukertype(BRUKER_TYPE)
                        .build())
                .antallSider(ANTALL_SIDER)
                .batchnavn(BATCH_NAVN)
                .datoMottatt(DATO_MOTTATT)
                .endorsernr(ENDORSERNR)
                .mottakskanal(MOTTAKSKANAL)
                .brevKode(BREVKODE)
                .filnavn(FILEBASENAME)
                .journalforendeEnhet(JOURNALFORENDEENHET)
                .land(LAND)
                .tema(TEMA);
    }


    static PostboksOvrigEnvelope.PostboksOvrigEnvelopeBuilder createMinimalEnvelope() {
        return PostboksOvrigEnvelope.builder()
                .filebasename(FILEBASENAME)
                .zipname(ZIPNAME)
                .skanningmetadata(Skanningmetadata.builder()
                        .journalpost(Journalpost.builder()
                                .mottakskanal(MOTTAKSKANAL)
                                .datoMottatt(DATO_MOTTATT)
                                .batchnavn(BATCH_NAVN)
                                .build())
                        .skanningInfo(SkanningInfo.builder()
                                .strekkodePostboks(STREKKODE_POSTBOKS)
                                .build())
                        .build())
                .xml(XML_FIL)
                .pdf(PDF_FIL);
    }
}
