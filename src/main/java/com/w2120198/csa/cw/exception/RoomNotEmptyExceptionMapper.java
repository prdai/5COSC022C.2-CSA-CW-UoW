package com.w2120198.csa.cw.exception;

import com.w2120198.csa.cw.model.ErrorMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    private static final String DOCS = "https://smartcampus.westminster.ac.uk/api/docs/errors#409";

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        ErrorMessage body = new ErrorMessage(exception.getMessage(), Status.CONFLICT.getStatusCode(), DOCS);
        return Response.status(Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
