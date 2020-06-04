package no.nav.skanmotovrig.validator;

import java.util.Date;

public class JournalpostValidator {

    public static boolean isValidMottakskanal(String mottakskanal) {
        return "SKAN_IM".equals(mottakskanal);
    }

    public static boolean isValidDatoMottatt(Date datoMottatt) {
        return null != datoMottatt;
    }

    public static boolean isValidBatchNavn(String batchnavn) {
        return isNonEmptyString(batchnavn);
    }

    public static boolean isValidFilnavn(String filnavn) {
        if(isNonEmptyString(filnavn) && filnavn.lastIndexOf(".") > 0){
            return filnavn.lastIndexOf(".") < filnavn.length() -1;
        }
        return false;
    }

    public static boolean isValidEndorsernr(String endorsernr) {
        return isNonEmptyString(endorsernr);
    }

    public static boolean isValidJournalfoerendeEnhet(String journalfoerendeEnhet) {
        return journalfoerendeEnhet == null || (isNumeric(journalfoerendeEnhet) && journalfoerendeEnhet.length() == 4);
    }

    public static boolean isValidReferansenummer(String referansenummer) {
        return referansenummer == null;
    }

    public static boolean isValidjournalpostId(String journalpostId) {
        return journalpostId == null;
    }

    private static boolean isNumeric(String string) {
        if (isNonEmptyString(string)) {
            try {
                Long.parseLong(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }

}
