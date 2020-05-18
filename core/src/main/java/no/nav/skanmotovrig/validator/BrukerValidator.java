package no.nav.skanmotovrig.validator;

public class BrukerValidator {
    private static final int BRUKERID_MINLENGTH = 9;
    private static final int BRUKERID_MAXLENGTH = 11;

    private static final String PERSON = "PERSON";
    private static final String ORGANISASJON = "ORGANISASJON";

    public static boolean isValidBrukerId(String brukerId) {
        return isNumeric(brukerId) && (BRUKERID_MINLENGTH <= brukerId.length() && brukerId.length() <= BRUKERID_MAXLENGTH);
    }

    public static boolean isValidBrukerType(String brukerId, String brukerType) {
        if(brukerId.length() == BRUKERID_MAXLENGTH){
            return PERSON.equals(brukerType);
        } else {
            return ORGANISASJON.equals(brukerType);
        }
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
