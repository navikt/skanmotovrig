package no.nav.skanmotovrig.exceptions.technical;

public class AbstractSkanmotovrigTechnicalException extends RuntimeException {

    public AbstractSkanmotovrigTechnicalException(String message) {
        super(message);
    }

    public AbstractSkanmotovrigTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
