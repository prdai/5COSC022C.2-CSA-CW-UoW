package com.w2120198.csa.cw.exception;

import com.w2120198.csa.cw.model.ErrorMessage;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final String DOCS = "https://smartcampus.westminster.ac.uk/api/docs/errors#422";

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ErrorMessage body = new ErrorMessage(exception.getMessage(), UNPROCESSABLE_ENTITY, DOCS);
        return Response.status(UNPROCESSABLE_ENTITY).entity(body).build();
    }
}
