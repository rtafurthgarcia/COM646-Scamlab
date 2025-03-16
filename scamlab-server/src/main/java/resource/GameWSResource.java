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
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.inject.Inject;
import model.dto.GameDTO.GameGameCancelledMessageDTO;
import model.dto.GameDTO.LeaveRequestDTO;
import model.dto.GameDTO.VoteStartRequestDTO;
import model.dto.GameDTO.WaitingLobbyAssignedStrategyMessageDTO;
import model.dto.GameDTO.WaitingLobbyGameStartingMessageDTO;
import model.dto.GameDTO.WaitingLobbyReadyToStartMessageDTO;
import model.dto.GameDTO.WaitingLobbyReasonForWaitingMessageDTO;
import model.dto.GameDTO.WaitingLobbyVoteToStartMessageDTO;
import model.entity.TransitionReason;

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
    @Channel("register-start-game")
    @Broadcast
    Emitter<VoteStartRequestDTO> registerStartGameEmitter;

    @Inject
    @Channel("handle-player-leaving")
    @Broadcast
    Emitter<LeaveRequestDTO> leaveEmitter;

    @OnOpen
    public void onOpen() {
        Log.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        Log.info("New player waiting to join a game: " + connection.id());
        registry.register(securityIdentity.getPrincipal().getName(), connection.id());
    }

    @Incoming("notify-evolution")
    public Uni<Void> notifyPlayersOfChange(WaitingLobbyReasonForWaitingMessageDTO statistics) {
        Log.info("Notified player " 
            + statistics.playerSecondaryId() 
            + " about the reasons why they are waiting: " 
            + String.join(", ", statistics.reasons()));

        return connectionManager.findByConnectionId(
            registry.getConnectionId(statistics.playerSecondaryId())
        ).get().sendText(statistics);
    }

    @Incoming("assign-new-role")
    public Uni<Void> notifyOfNewlyAssignedRole(WaitingLobbyAssignedStrategyMessageDTO message) {
        Log.info("Role " 
            + message.role()  
            + " assigned for player " 
            + message.playerSecondaryId() 
            + " part of game " 
            + message.conversationSecondaryId());

        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
            ).get().sendText(message);
            
    }

    @Incoming("notify-game-as-ready")
    public Uni<Void> setGameAsReady(WaitingLobbyReadyToStartMessageDTO message) {
        Log.info("Player " + message.playerSecondaryId() + " notified that their game is ready");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("notify-game-as-starting")
    public Uni<Void> startGame(WaitingLobbyGameStartingMessageDTO message) {
        Log.info("Player " + message.playerSecondaryId() + " notified that their game is starting");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        leaveEmitter.send(
            new LeaveRequestDTO(
                UUID.fromString(securityIdentity.getPrincipal().getName()),
                TransitionReason.ConnectionGotTerminated
            )
        );
        registry.unregister(securityIdentity.getPrincipal().getName());
    }

    @OnTextMessage
    public void processAsync(Record message) {
        if (message instanceof WaitingLobbyVoteToStartMessageDTO) {
            var conversationId = UUID.fromString(((WaitingLobbyVoteToStartMessageDTO) message).conversationSecondaryId());
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            registerStartGameEmitter.send(
                new VoteStartRequestDTO(
                    playerId, 
                    conversationId
                )
            );
        }

        if (message instanceof GameGameCancelledMessageDTO) {
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            leaveEmitter.send(
                new LeaveRequestDTO(
                    playerId,
                    TransitionReason.PlayerWillingfullyCancelled
                )
            );
        }
    }
}
