package no.nav.skanmotovrig.exceptions.functional;

/**
 * Brukes når en inngående forsendelse ikke er komplett.
 * Eks mangler zip, xml eller ocr.
 */
public class ForsendelseNotCompleteException extends AbstractSkanmotovrigFunctionalException {
    public ForsendelseNotCompleteException(String message) {
        super(message);
    }
}
