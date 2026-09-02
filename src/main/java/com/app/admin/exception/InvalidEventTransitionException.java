package com.app.admin.exception;

public class InvalidEventTransitionException extends RuntimeException {
    public InvalidEventTransitionException(String message) {
        super(message);
    }
}
