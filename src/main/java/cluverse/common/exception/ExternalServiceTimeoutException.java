package cluverse.common.exception;

public class ExternalServiceTimeoutException extends RuntimeException {

    public ExternalServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
