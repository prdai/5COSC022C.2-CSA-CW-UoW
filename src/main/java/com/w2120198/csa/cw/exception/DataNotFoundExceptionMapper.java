package com.w2120198.csa.cw.exception;

import com.w2120198.csa.cw.model.ErrorMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DataNotFoundExceptionMapper implements ExceptionMapper<DataNotFoundException> {

    private static final String DOCS = "https://smartcampus.westminster.ac.uk/api/docs/errors#404";

    @Override
    public Response toResponse(DataNotFoundException exception) {
        ErrorMessage body = new ErrorMessage(exception.getMessage(), Status.NOT_FOUND.getStatusCode(), DOCS);
        return Response.status(Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
