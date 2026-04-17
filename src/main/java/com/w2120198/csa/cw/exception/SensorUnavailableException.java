package com.w2120198.csa.cw.exception;

/**
 * Raised when an operation targets a sensor whose current state blocks
 * it — posting a reading to a sensor marked {@code MAINTENANCE}, for
 * example. Mapped to HTTP 403 Forbidden: the server understands the
 * request and could fulfil it in principle, but refuses because of
 * the sensor's state rather than the client's identity.
 */
public class SensorUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SensorUnavailableException(String message) {
        super(message);
    }
}
