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
import model.dto.MessageDTODecoder;
import model.dto.GameDTO.GameGameCancelledMessageDTO;
import model.dto.GameDTO.LeaveRequestInternalDTO;
import model.dto.GameDTO.VoteAcknowledgedMessageDTO;
import model.dto.GameDTO.VoteStartRequestInternalDTO;
import model.dto.GameDTO.WaitingLobbyGameAssignmentMessageDTO;
import model.dto.GameDTO.WaitingLobbyGameStartingMessageDTO;
import model.dto.GameDTO.WaitingLobbyReadyToStartMessageDTO;
import model.dto.GameDTO.WaitingLobbyReasonForWaitingMessageDTO;
import model.dto.GameDTO.WaitingLobbyVoteToStartMessageDTO;
import model.entity.TransitionReason;

@Authenticated
@WebSocket(path = "/ws/lobby")
public class LobbyWSResource {
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
    Emitter<VoteStartRequestInternalDTO> registerStartGameEmitter;

    @Inject
    @Channel("handle-player-leaving")
    @Broadcast
    Emitter<LeaveRequestInternalDTO> leaveEmitter;

    @OnOpen
    public void onOpen() {
        Log.info(securityIdentity.getPrincipal().getName() + " successfully authenticated");
        Log.info("New player waiting to join a game: " + connection.id());
        registry.putIfAbsent(securityIdentity.getPrincipal().getName(), connection.id());
    }

    @Incoming("notify-reason-for-waiting")
    public Uni<Void> notifyPlayersOfChange(WaitingLobbyReasonForWaitingMessageDTO statistics) {
        Log.info("Notified player " 
            + statistics.playerSecondaryId() 
            + " about the reasons why they are waiting: " 
            + String.join(", ", statistics.reasons()));

        return connectionManager.findByConnectionId(
            registry.get(statistics.playerSecondaryId())
        ).get().sendText(statistics);
    }

    @Incoming("return-game-assignment")
    public Uni<Void> notifyOfNewlyAssignedRole(WaitingLobbyGameAssignmentMessageDTO message) {
        Log.info("Role " 
            + message.role()  
            + " assigned for player " 
            + message.playerSecondaryId() 
            + " part of game " 
            + message.conversationSecondaryId());

        return connectionManager.findByConnectionId(
            registry.get(message.playerSecondaryId())
            ).get().sendText(message);
            
    }

    @Incoming("notify-game-as-ready")
    public Uni<Void> setGameAsReady(WaitingLobbyReadyToStartMessageDTO message) {
        Log.info("Player " + message.playerSecondaryId() + " notified that their game is ready");
        
        return connectionManager.findByConnectionId(
            registry.get(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("acknowledge-start-vote")
    public Uni<Void> acknowledgeStartVote(VoteAcknowledgedMessageDTO message) {
        Log.info("Notify player " + message.playerSecondaryId() + " that their start vote has been acknowledged");
        
        return connectionManager.findByConnectionId(
            registry.get(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("notify-game-as-starting")
    public Uni<Void> startGame(WaitingLobbyGameStartingMessageDTO message) {
        Log.info("Notify player " + message.playerSecondaryId() + " that their game is starting");
        
        return connectionManager.findByConnectionId(
            registry.get(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        
        // Won't send the leave request for players who will reconnect
        leaveEmitter.send(
            new LeaveRequestInternalDTO(
                UUID.fromString(securityIdentity.getPrincipal().getName()),
                TransitionReason.ConnectionGotTerminated
            )
        );        
    }

    @OnTextMessage(codec = MessageDTODecoder.class)
    public void processAsync(Record message) {
        // For players who will reconnect right away to the /games/ endpoint
        if (message instanceof WaitingLobbyGameStartingMessageDTO) {
            var playerId = securityIdentity.getPrincipal().getName();
            Log.info("Game starting for player " + securityIdentity.getPrincipal().getName());
            registry.remove(playerId);
        }

        if (message instanceof WaitingLobbyVoteToStartMessageDTO) {
            var conversationId = UUID.fromString(((WaitingLobbyVoteToStartMessageDTO) message).conversationSecondaryId());
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            registerStartGameEmitter.send(
                new VoteStartRequestInternalDTO(
                    playerId, 
                    conversationId
                )
            );
        }

        if (message instanceof GameGameCancelledMessageDTO) {
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            leaveEmitter.send(
                new LeaveRequestInternalDTO(
                    playerId,
                    TransitionReason.PlayerWillingfullyCancelled
                )
            );
        }
    }
}
