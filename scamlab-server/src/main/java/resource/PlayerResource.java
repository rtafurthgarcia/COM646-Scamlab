package resource;

import org.jboss.logmanager.Logger;
import io.vertx.mutiny.core.http.HttpServerRequest;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import service.PlayerService;

@Path("players")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Dependent
public class PlayerResource {
    public final static Logger LOGGER = Logger.getLogger(PlayerService.class.getSimpleName());

    public enum SystemRole {
        PLAYER,
        ADMIN;
    }

    @Inject
    PlayerService service;

    @GET
    @Path("join")
    @Transactional
    public void join(@Context HttpServerRequest request) {
        String clientIP = request.remoteAddress().hostAddress();
        LOGGER.info("IP Address: " + clientIP);;    
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
