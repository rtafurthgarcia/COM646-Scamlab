package resource;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import model.dto.PlayerMapper;
import model.dto.PlayerDto.GetNewPlayerDto;
import model.entity.Player;
import service.PlayerService;

@Path("players")
public class PlayerResource {
    @Inject
    Logger logger;

    @Inject
    PlayerService service;

    @Inject 
    PlayerMapper mapper;

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
        String clientIP = routingContext.request().remoteAddress().host();

        Player player = service.registerNewPlayer(clientIP);

        return Response
            .status(Status.CREATED)
            .entity(mapper.toGetNewPlayerDto(player, 0))
            .build();
    }

    /* 
    @GET
    @Path("/login")
    public String login(@QueryParam("login")String login, @QueryParam("password") String password) {
        User existingUser = User.find("login", login).firstResult();
        if(existingUser == null || !existingUser.password.equals(password)) {
            throw new WebApplicationException(Response.status(404).entity("No user found or password is incorrect").build());
        }
        return service.generateUserToken(existingUser.email, password);
    }*/
} 
