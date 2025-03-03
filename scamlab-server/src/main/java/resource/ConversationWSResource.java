package resource;

import org.jboss.logging.Logger;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import jakarta.inject.Inject;
import service.PlayerService;

@Authenticated
@WebSocket(path = "/ws/conversation/start")
public class ConversationWSResource {
    public record ChatMessage(long numberOfPlayersConnected) {}

    @Inject
    PlayerService service;

    @Inject
    Logger logger;

    @Inject
    WebSocketConnection connection;

    @Inject
    ConnectionManager connectionManager;

    @Inject
    SecurityIdentity securityIdentity;

    @OnOpen
    public ChatMessage onOpen() {
        logger.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        logger.info("New WS connection: " + connection.endpointId());
        return new ChatMessage(connectionManager.getConnections("*").size());
    }

    @OnClose
    public void onClose() {
        logger.info("WS connection closed: " + connection.endpointId());
        connection.broadcast().sendTextAndAwait(new ChatMessage(connectionManager.getConnections("*").size()));
    }
}
