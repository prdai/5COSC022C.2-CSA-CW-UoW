package com.w2120198.csa.cw.exception;

public class SensorUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SensorUnavailableException(String message) {
        super(message);
    }
}
