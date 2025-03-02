package exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PlayerExceptionMapper implements ExceptionMapper<PlayerException> {

    @Override
    public Response toResponse(PlayerException exception) {
        return Response.status(Response.Status.CONFLICT)
            .entity(new ErrorResponse(exception.getMessage()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
    
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
