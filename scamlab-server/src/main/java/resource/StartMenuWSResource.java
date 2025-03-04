package resource;

import org.jboss.logging.Logger;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import jakarta.inject.Inject;

@Authenticated
@WebSocket(path = "/ws/start-menu")
public class StartMenuWSResource {
    public record ChatMessage(long numberOfPlayersConnected) {}

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
        return new ChatMessage(connectionManager.listAll().size());
    }

    @OnClose
    public void onClose() {
        logger.info("WS connection closed: " + connection.endpointId());
        connection.broadcast().sendTextAndAwait(new ChatMessage(connectionManager.listAll().size()));
    }
}
