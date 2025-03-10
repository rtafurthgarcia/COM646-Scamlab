package resource;

import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import helper.PlayerConnectionRegistry;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import model.dto.GameDto;
import model.dto.GameDto.WaitingLobbyAssignedStrategyMessageDto;
import model.dto.GameDto.WaitingLobbyReadyToStartMessageDto;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import model.dto.GameDto.WaitingLobbyVoteToStartMessageDto;
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

    @Incoming("notify-evolution-out")
    public void notifyPlayersOfChange(WaitingLobbyStatisticsMessageDto statistics) {
        connectionManager.listAll().forEach(c -> c.sendTextAndAwait(statistics));
    }

    @Incoming("new-assigned-role-out")
    public void notifyOfNewlyAssignedRole(WaitingLobbyAssignedStrategyMessageDto message) {
        connectionManager.listAll().forEach(c -> c.sendTextAndAwait(message));
    }

    @Incoming("game-ready-out")
    public void setGameAsReady(Conversation conversation) {
        /*connectionManager.listAll().forEach(c -> c.broadcast().sendTextAndAwait(
            new GameDto.Wait(

            )));*/
        Log.info("Game ready!");
    }

    @Incoming("game-starting-out")
    public void startGame(Conversation conversation) {
        /*connectionManager.listAll().forEach(c -> c.broadcast().sendTextAndAwait(
            new GameDto.Wait(

            )));*/
            Log.info("Game starting!");
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        registry.unregister(securityIdentity.getPrincipal().getName());
        connection.broadcast().sendTextAndAwait(service.getWaitingLobbyStatistics());
    }

    @OnTextMessage
    @RunOnVirtualThread
    public void processAsync(Record message) {
        if (message instanceof WaitingLobbyVoteToStartMessageDto) {
            var conversationId = UUID.fromString(((WaitingLobbyVoteToStartMessageDto) message).conversationSecondaryId());
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());
            var player = service.findUserBySecondaryId(playerId);
            var conversation = service.findConversationBySecondaryId(conversationId);

            service.registerStartGame(conversation, player);
        }
    }
}
