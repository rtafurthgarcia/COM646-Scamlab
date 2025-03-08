package resource;

import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import jakarta.inject.Inject;
import model.dto.GameDto.StartMenuStatisticsMessageDto;
import model.dto.GameDto.WSMessageType;

@Authenticated
@WebSocket(path = "/ws/start-menu")
public class StartMenuWSResource {
    @Inject
    WebSocketConnection connection;

    @Inject
    ConnectionManager connectionManager;

    @Inject
    SecurityIdentity securityIdentity;

    @OnOpen
    public StartMenuStatisticsMessageDto onOpen() {
        Log.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        Log.info("New WS connection: " + connection.endpointId());
        return new StartMenuStatisticsMessageDto(WSMessageType.NOTIFY_START_MENU_STATISTICS, connectionManager.listAll().size());
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        connection.broadcast().sendTextAndAwait(new StartMenuStatisticsMessageDto(WSMessageType.NOTIFY_START_MENU_STATISTICS, connectionManager.listAll().size()));
    }
}
