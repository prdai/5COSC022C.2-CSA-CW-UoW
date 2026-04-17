package com.w2120198.csa.cw.exception;

/**
 * Thrown when a lookup by id finds no matching entity. Mapped to
 * HTTP 404 Not Found by {@link DataNotFoundExceptionMapper}.
 */
public class DataNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataNotFoundException(String message) {
        super(message);
    }
}
