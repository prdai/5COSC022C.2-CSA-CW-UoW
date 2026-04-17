package com.w2120198.csa.cw.exception;

import com.w2120198.csa.cw.model.ErrorMessage;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    private static final String DOCS = "https://smartcampus.westminster.ac.uk/api/docs/errors#403";

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ErrorMessage body = new ErrorMessage(exception.getMessage(), Status.FORBIDDEN.getStatusCode(), DOCS);
        return Response.status(Status.FORBIDDEN).entity(body).build();
    }
}
