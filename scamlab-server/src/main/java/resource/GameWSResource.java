package resource;

import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
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
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.inject.Inject;
import model.dto.GameDto.CancellationRequestDto;
import model.dto.GameDto.GameGameCancelledMessageDto;
import model.dto.GameDto.VoteStartRequestDto;
import model.dto.GameDto.WaitingLobbyAssignedStrategyMessageDto;
import model.dto.GameDto.WaitingLobbyGameStartingMessageDto;
import model.dto.GameDto.WaitingLobbyReadyToStartMessageDto;
import model.dto.GameDto.WaitingLobbyReasonForWaitingMessageDto;
import model.dto.GameDto.WaitingLobbyVoteToStartMessageDto;
import model.entity.TransitionReason;

@Authenticated
@RunOnVirtualThread
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
    @Channel("register-start-game")
    @Broadcast
    Emitter<VoteStartRequestDto> registerStartGameEmitter;

    @Inject
    @Channel("cancel-if-necessary")
    @Broadcast
    Emitter<CancellationRequestDto> cancelEmitter;

    @OnOpen
    public void onOpen() {
        Log.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        Log.info("New player waiting to join a game: " + connection.id());
        registry.register(securityIdentity.getPrincipal().getName(), connection.id());
    }

    @Incoming("notify-evolution")
    public void notifyPlayersOfChange(WaitingLobbyReasonForWaitingMessageDto statistics) {
        connectionManager.listAll().forEach(c -> c.sendTextAndAwait(statistics));
    }

    @Incoming("assign-new-role")
    public void notifyOfNewlyAssignedRole(WaitingLobbyAssignedStrategyMessageDto message) {
        connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendTextAndAwait(message);

        Log.info("Role " 
            + message.role()  
            + " assigned for player " 
            + message.playerSecondaryId() 
            + " part of game " 
            + message.conversationSecondaryId());
    }

    @Incoming("notify-game-as-ready")
    public void setGameAsReady(WaitingLobbyReadyToStartMessageDto message) {
        connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendTextAndAwait(message);

        Log.info("Player " + message.playerSecondaryId() + " notified that their game is ready");
    }

    @Incoming("notify-game-as-starting")
    public void startGame(WaitingLobbyGameStartingMessageDto message) {
        connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendTextAndAwait(message);

        Log.info("Player " + message.playerSecondaryId() + " notified that their game is starting");
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        registry.unregister(securityIdentity.getPrincipal().getName());
        cancelEmitter.send(
            new CancellationRequestDto(
                UUID.fromString(securityIdentity.getPrincipal().getName()),
                TransitionReason.ConnectionGotTerminated
            )
        );
    }

    @OnTextMessage
    public void processAsync(Record message) {
        if (message instanceof WaitingLobbyVoteToStartMessageDto) {
            var conversationId = UUID.fromString(((WaitingLobbyVoteToStartMessageDto) message).conversationSecondaryId());
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            registerStartGameEmitter.send(
                new VoteStartRequestDto(
                    playerId, 
                    conversationId
                )
            );
        }

        if (message instanceof GameGameCancelledMessageDto) {
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            cancelEmitter.send(
                new CancellationRequestDto(
                    playerId,
                    TransitionReason.PlayerWillingfullyCancelled
                )
            );
        }
    }
}
