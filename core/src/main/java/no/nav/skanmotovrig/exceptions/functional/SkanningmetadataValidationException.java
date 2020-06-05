package no.nav.skanmotovrig.exceptions.functional;

/**
 * Brukes når en skanningmetadata xml ikke validerer.
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
public class SkanningmetadataValidationException extends AbstractSkanmotovrigFunctionalException {
    public SkanningmetadataValidationException(String message) {
        super(message);
    }

    public SkanningmetadataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
