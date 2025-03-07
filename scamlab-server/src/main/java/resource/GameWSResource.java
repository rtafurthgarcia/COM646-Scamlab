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
import model.dto.GameDto;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import service.GameService;

@Authenticated
@WebSocket(path = "/ws/games")
public class GameWSResource {
    @Inject
    Logger logger;

    @Inject
    WebSocketConnection connection;

    @Inject
    ConnectionManager connectionManager;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    GameService service;

    @OnOpen
    public WaitingLobbyStatisticsMessageDto onOpen() {
        logger.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        logger.info("New player waiting to join a game: " + connection.endpointId());

        return service.getWaitingLobbyStatistics();
    }

    @OnClose
    public void onClose() {
        logger.info("WS connection closed: " + connection.endpointId());
        connection.broadcast().sendTextAndAwait(service.getWaitingLobbyStatistics());
    }
}
