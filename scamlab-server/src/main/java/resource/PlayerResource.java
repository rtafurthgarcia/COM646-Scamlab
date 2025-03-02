package resource;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

import exception.PlayerException;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import model.dto.PlayerMapper;
import model.dto.PlayerDto.GetNewPlayerDto;
import service.PlayerService;

@Path("players")
@PermitAll
public class PlayerResource {
    @Inject
    Logger logger;

    @Inject
    PlayerService service;

    @Inject 
    PlayerMapper mapper;

    @Context
    SecurityContext securityContext;

    @GET
    @Path("join")
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
    @Transactional
    public Response join(@Context RoutingContext routingContext) {
        var clientIP = routingContext.request().remoteAddress().host();

        var player = service.registerNewPlayer(clientIP);

        return Response
            .status(Status.CREATED)
            .entity(mapper.toGetNewPlayerDto(player, 0))
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
    @Transactional
    @Authenticated
    public Response leave(String secondaryId) {
    
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
