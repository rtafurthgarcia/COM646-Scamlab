package resource;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import exception.PlayerException;
import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import model.dto.AuthenticationMapper;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import service.AuthenticationService;

@Path("players")
@PermitAll
public class AuthenticationResource {
    @Inject
    AuthenticationService service;

    @Inject 
    AuthenticationMapper mapper;

    @Context
    SecurityContext securityContext;

    @Inject
    ConnectionManager connectionManager;

    @GET
    @Path("new")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201",
            description = "New player successfully registered", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = GetNewPlayerDto.class)
            )
        )
    })
    public Response register(@Context RoutingContext routingContext) {
        var clientIP = routingContext.request().remoteAddress().host();

        var player = service.registerNewPlayer(clientIP);

        return Response
            .status(Status.CREATED)
            .entity(mapper.toGetNewPlayerDto(player))
            .build();
    }

    @DELETE
    @Path("{secondaryId}")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "205",
            description = "Player successfully unregistered"
        )
    })
    @Authenticated
    public Response clearToken(String secondaryId) {
    
        if (securityContext.getUserPrincipal().getName().equals(secondaryId)) {
            service.unregisterPlayersToken(service.findUserBySecondaryId(UUID.fromString(secondaryId)));

            return Response
                .status(205)
                .build();
        } else {
            throw new PlayerException("Cannot unregister a player that has never been registered before");
        }
    }
} 
