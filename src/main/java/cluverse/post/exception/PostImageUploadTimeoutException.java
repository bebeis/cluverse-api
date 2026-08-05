package cluverse.post.exception;

import cluverse.common.exception.ExternalServiceTimeoutException;

public class PostImageUploadTimeoutException extends ExternalServiceTimeoutException {

    public PostImageUploadTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
