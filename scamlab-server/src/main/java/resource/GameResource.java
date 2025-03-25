package resource;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import model.dto.AuthenticationDTO.GetNewPlayerDTO;
import model.dto.GameDTO.GameReconcileStateMessageDTO;
import model.entity.Player;
import service.LobbyService;
import service.AuthenticationService;

@Path("games")
public class GameResource {
    @Context
    SecurityContext securityContext;

    @Inject
    AuthenticationService authenticationService;

    @Inject
    LobbyService gameService;

    @Inject
    ConnectionManager connectionManager;

    @Inject
    @Channel("put-players-on-waiting-list")
    @Broadcast
    Emitter<Player> putPlayersOnWaitingListEmitter;

    @GET
    @Path("join")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "New game successfully registered", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = GetNewPlayerDTO.class)
            )
        )
    })
    @Authenticated
    @RunOnVirtualThread
    public Response join() {
        var player = authenticationService.findUserBySecondaryId(UUID.fromString(securityContext.getUserPrincipal().getName()));        

        putPlayersOnWaitingListEmitter.send(player);

        return Response
            .ok()
            .build();
    }

    @GET
    @Path("{id}/state")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Current game's state returned", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = GameReconcileStateMessageDTO.class)
            )
        )
    })
    @Authenticated
    @RunOnVirtualThread
    public Response reconcile(String id) {
        var playerId = securityContext.getUserPrincipal().getName();
        var message = gameService.reconcileStateForClient(id, playerId);
        Log.info("Reconcilating the state for game " + id + " for player " + playerId);

        return Response.ok().entity(message).status(200).build();
    }
}
