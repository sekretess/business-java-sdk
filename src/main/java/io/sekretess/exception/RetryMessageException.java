package io.sekretess.exception;

public class RetryMessageException extends RuntimeException {
    public RetryMessageException(String message) {
        super(message);
    }
}
