package com.banking.exception;

public class InvalidAccountOperationException extends RuntimeException {
    public InvalidAccountOperationException(String message) {
        super(message);
    }

    public InvalidAccountOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
