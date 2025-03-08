package resource;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import service.GameService;
import service.AuthenticationService;

@Path("games")
public class GameResource {
    @Context
    SecurityContext securityContext;

    @Inject
    AuthenticationService authenticationService;

    @Inject
    GameService conversationService;

    @Inject
    ConnectionManager connectionManager;

    @GET
    @Path("join")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201",
            description = "New game successfully registered", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = GetNewPlayerDto.class)
            )
        )
    })
    @Authenticated
    public Response join() {
        var player = authenticationService.findUserBySecondaryId(UUID.fromString(securityContext.getUserPrincipal().getName()));        

        conversationService.putPlayerOnWaitingList(player);

        return Response
            .ok()
            .build();
    }
}
