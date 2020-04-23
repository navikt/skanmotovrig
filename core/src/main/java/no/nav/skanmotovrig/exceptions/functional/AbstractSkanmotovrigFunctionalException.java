package no.nav.skanmotovrig.exceptions.functional;

public class AbstractSkanmotovrigFunctionalException extends RuntimeException {

    public AbstractSkanmotovrigFunctionalException(String message) {
        super(message);
    }

    public AbstractSkanmotovrigFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
