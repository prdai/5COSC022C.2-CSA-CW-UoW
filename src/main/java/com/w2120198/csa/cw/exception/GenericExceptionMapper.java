package com.w2120198.csa.cw.exception;

import com.w2120198.csa.cw.model.ErrorMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Catch-all mapper for anything no more specific mapper has claimed.
 * Logs the full {@link Throwable} on the server and returns a generic
 * 500 body to the client, so stack traces, framework versions, and
 * internal paths never reach external consumers.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());
    private static final String DOCS = "https://smartcampus.westminster.ac.uk/api/docs/errors#500";

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Unhandled error bubbled to global mapper", exception);
        ErrorMessage body = new ErrorMessage(
                "The server encountered an unexpected error. Please retry or contact support.",
                Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                DOCS);
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
