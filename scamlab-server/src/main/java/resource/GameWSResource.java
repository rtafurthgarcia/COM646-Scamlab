package resource;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import helper.PlayerConnectionRegistry;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.dto.GameDto;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import model.entity.Conversation;
import model.entity.Player;
import service.GameService;

@RunOnVirtualThread
@Authenticated
@WebSocket(path = "/ws/games")
public class GameWSResource {
    @Inject
    WebSocketConnection connection;

    @Inject
    ConnectionManager connectionManager;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    PlayerConnectionRegistry registry;

    @Inject
    GameService service;

    @OnOpen
    public WaitingLobbyStatisticsMessageDto onOpen() {
        Log.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        Log.info("New player waiting to join a game: " + connection.id());
        registry.register(securityIdentity.getPrincipal().getName(), connection.id());

        return service.getWaitingLobbyStatistics();
    }

    @Incoming("player-joined-game-out")
    @Transactional
    public void addPlayerToNewGame(Player player) {
        Log.info("Incoming message received for player " + player.getSecondaryId());
        connectionManager.findByConnectionId(
            registry.getConnectionId(player.getSecondaryId().toString()))
            .get().sendTextAndAwait(service.getWaitingLobbyStatistics());

        service.prepareNewGame(player);
    }

    @Incoming("notify-evolution-out")
    public void notifyPlayersOfChange(Conversation conversation) {
        var message = service.getWaitingLobbyStatistics();

        connectionManager.listAll().forEach(c -> c.broadcast().sendTextAndAwait(message));
    }

    @Incoming("game-ready-out")
    public void startGame(Conversation conversation) {
        connectionManager.listAll().forEach(c -> c.broadcast().sendTextAndAwait(new GameDto.WaitingLobbyVoteToStartMessageDto()));
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        registry.unregister(securityIdentity.getPrincipal().getName());
        connection.broadcast().sendTextAndAwait(service.getWaitingLobbyStatistics());
    }

    /*@OnTextMessage
    @RunOnVirtualThread
    Uni<ResponseMessage> processAsync(Message m) {
    // Process the incoming message and send a response to the client.
    // The method is called for each incoming message.
    // Note that if the method returns `null`, no response will be sent to the client. The method completes when the returned Uni emits its item.
    }*/
}
