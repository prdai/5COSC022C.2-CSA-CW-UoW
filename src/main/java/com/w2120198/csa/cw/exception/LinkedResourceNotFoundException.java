package com.w2120198.csa.cw.exception;

/**
 * Raised when a client submits a well-formed JSON payload that refers
 * to another resource which does not exist — typically a sensor whose
 * {@code roomId} points at an unknown room. Mapped to HTTP 422
 * Unprocessable Entity because the request is syntactically valid but
 * semantically broken.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
