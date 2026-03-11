package cluverse.common.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super(ExceptionMessage.UNAUTHORIZED.getMessage());
    }

    public UnauthorizedException(ExceptionMessage exceptionMessage) {
        super(exceptionMessage.getMessage());
    }
}
