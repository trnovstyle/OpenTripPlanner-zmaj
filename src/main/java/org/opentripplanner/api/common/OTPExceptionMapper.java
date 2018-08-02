package org.opentripplanner.api.common;

import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
public class OTPExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = LoggerFactory.getLogger(OTPExceptionMapper.class);


    private Map<Class<? extends Exception>, Integer> responseStatusPerExceptionClass = new HashMap<>();

    public OTPExceptionMapper() {

        responseStatusPerExceptionClass.put(JsonParseException.class, Response.Status.BAD_REQUEST.getStatusCode());
    }

    public Response toResponse(Exception ex) {
        int statusCode = getStatusForException(ex);

        if (statusCode >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            // Show the exception in the server log
            LOG.error("Unhandled exception", ex);
        } else {
            LOG.debug("Client exception", ex);
        }

        // Return the short form message to the client
        return Response.status(statusCode)
                       .entity(ex.getMessage())
                       .type("text/plain").build();
    }

    private <E extends Exception> int getStatusForException(E ex) {
        int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(); // Default value

        if (ex != null) {
            if (ex instanceof WebApplicationException) {
                statusCode = ((WebApplicationException) ex).getResponse().getStatus();
            } else {
                statusCode = responseStatusPerExceptionClass.entrySet().stream()
                                     .filter(exClass -> exClass.getKey().isAssignableFrom(ex.getClass())).map(Map.Entry::getValue).findFirst().orElse(statusCode);
            }
        }

        return statusCode;
    }

}
