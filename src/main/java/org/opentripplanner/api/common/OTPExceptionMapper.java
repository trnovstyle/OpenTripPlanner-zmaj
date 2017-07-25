package org.opentripplanner.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class OTPExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = LoggerFactory.getLogger(OTPExceptionMapper.class);

    public Response toResponse(Exception ex) {
        // Show the exception in the server log
        LOG.error("Unhandled exception", ex);

        int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        if (ex instanceof WebApplicationException)
            statusCode = ((WebApplicationException)ex).getResponse().getStatus();

        // Return the short form message to the client
        return Response.status(statusCode)
                .entity(ex.toString() + " " + ex.getMessage())
                .type("text/plain").build();
    }

}
