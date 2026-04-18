package com.w2120198.csa.cw.exception;

public class RoomNotEmptyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
