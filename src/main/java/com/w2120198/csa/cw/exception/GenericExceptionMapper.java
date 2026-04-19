package com.w2120198.csa.cw.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.w2120198.csa.cw.model.ErrorMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Fallback mapper. WebApplicationException keeps its original HTTP status,
 * Jackson parse failures become 400, anything else becomes a generic 500
 * so no stack trace leaks to the client.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        int status;
        String message;

        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            status = wae.getResponse().getStatus();
            message = wae.getMessage() != null ? wae.getMessage() : "Request could not be completed.";
        } else if (exception instanceof JsonProcessingException) {
            status = 400;
            message = "Request body was not valid JSON.";
        } else {
            LOGGER.log(Level.SEVERE, "Unhandled error bubbled to global mapper", exception);
            status = 500;
            message = "The server encountered an unexpected error.";
        }

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(message, status))
                .build();
    }
}
