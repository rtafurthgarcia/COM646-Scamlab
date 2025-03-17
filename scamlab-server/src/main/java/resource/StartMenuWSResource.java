package resource;

import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import model.dto.GameDTO.StartMenuStatisticsMessageDTO;

@Authenticated
@WebSocket(path = "/ws/start-menu")
public class StartMenuWSResource {
    @Inject
    WebSocketConnection connection;

    @Inject
    OpenConnections connections;

    @Inject
    SecurityIdentity securityIdentity;

    @OnOpen
    public void onOpen() {
        Log.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        Log.info("New WS connection: " + connection.endpointId());
        connection.broadcast().sendTextAndAwait(new StartMenuStatisticsMessageDTO(connections.listAll().size()));
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        connection.broadcast().sendTextAndAwait(new StartMenuStatisticsMessageDTO(connections.listAll().size()));
    }
}
