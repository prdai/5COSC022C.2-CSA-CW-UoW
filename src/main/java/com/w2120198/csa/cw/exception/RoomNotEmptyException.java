package com.w2120198.csa.cw.exception;

/**
 * Raised when a client attempts to delete a room that still has active
 * sensors assigned. Mapped to HTTP 409 Conflict so the client understands
 * the request is refused because of current resource state, not because
 * of a syntactic flaw in the request.
 */
public class RoomNotEmptyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
