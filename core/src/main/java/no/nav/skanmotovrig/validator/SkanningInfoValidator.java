package no.nav.skanmotovrig.validator;

import java.util.List;

public class SkanningInfoValidator {

    public static boolean isValidFysiskPostboks(String fysiskPostboks) {
        return isNonEmptyString(fysiskPostboks);
    }

    public static boolean isValidStrekkodePostboks(String strekkodePostboks) {
        return List.of("1406", "1407", "1410", "1423", "1431", "1234", "1442").contains(strekkodePostboks);
    }

    private static boolean isNonEmptyString(String string) {
        if (null != string) {
            return string.length() > 0;
        }
        return false;
    }
}
