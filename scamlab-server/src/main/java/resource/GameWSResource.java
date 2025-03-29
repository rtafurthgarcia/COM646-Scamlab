package resource;

import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import helper.PlayerConnectionRegistry;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.dto.MessageDTODecoder;
import model.dto.GameDTO.GameCallToVoteMessageDTO;
import model.dto.GameDTO.GameCancelledMessageDTO;
import model.dto.GameDTO.GameCastVoteMessageDTO;
import model.dto.GameDTO.GameFinishedMessageDTO;
import model.dto.GameDTO.GamePlayersMessageDTO;
import model.dto.GameDTO.GameStartingOrContinuingMessageDTO;
import model.dto.GameDTO.LeaveRequestInternalDTO;
import model.dto.GameDTO.VoteAcknowledgedMessageDTO;
import model.entity.TransitionReason;

@Authenticated
@ApplicationScoped
@WebSocket(path = "/ws/games/{conversationSecondaryId}")
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
    @Channel("handle-player-leaving-game")
    @Broadcast
    Emitter<LeaveRequestInternalDTO> leaveEmitter;

    @Inject
    @Channel("reply-received")
    @Broadcast
    Emitter<GamePlayersMessageDTO> replyReceivedEmitter;

    /*@Inject
    @Channel("start-inactivity-timeout")
    @Broadcast
    Emitter<String> startInactivityTimeoutEmitter;*/

    @Inject
    @Channel("register-vote")
    @Broadcast
    Emitter<GameCastVoteMessageDTO> registerVoteEmitter;

    @OnOpen
    public void onOpen() {
        Log.info("Player " + securityIdentity.getPrincipal().getName() + "joined conversation: " + connection.pathParam("conversationSecondaryId"));
        registry.register(securityIdentity.getPrincipal().getName(), connection.id());

        // provokes too much chaos
        //startInactivityTimeoutEmitter.send(securityIdentity.getPrincipal().getName());
    }

    @Incoming("send-reply")
    public Uni<Void> sendReply(GamePlayersMessageDTO message) {
        Log.info("Player " + message.receiverSecondaryId() + " is about to receive a message from Player " + message.senderSecondaryId());
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.receiverSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("notify-reason-for-abrupt-end-of-game")
    public Uni<Void> notifyOfGameCancellation(GameCancelledMessageDTO message) {
        Log.info("Notifying player " + message.playerSecondaryId() + " that their game has been interrupted");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("call-to-vote")
    public Uni<Void> callToVote(GameCallToVoteMessageDTO message) {
        Log.info("Calling player " + message.playerSecondaryId() + " to vote");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("acknowledge-vote")
    public Uni<Void> acknowledgeVote(VoteAcknowledgedMessageDTO message) {
        Log.info("Notify player " + message.playerSecondaryId() + " that their start vote has been acknowledged");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("notify-game-as-continuing")
    public Uni<Void> continueGame(GameStartingOrContinuingMessageDTO message) {
        Log.info("Notify player " + message.playerSecondaryId() + " that the game is continuing");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @Incoming("declare-game-as-finished")
    public Uni<Void> declareGameAsFinished(GameFinishedMessageDTO message) {
        Log.info("Notify player " + message.playerSecondaryId() + " that the game is over");
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.playerSecondaryId())
        ).get().sendText(message);
    }

    @OnClose
    public void onClose() {
        Log.info("WS connection closed: " + connection.endpointId());
        leaveEmitter.send(
            new LeaveRequestInternalDTO(
                UUID.fromString(securityIdentity.getPrincipal().getName()),
                TransitionReason.ConnectionGotTerminated
            )
        );
        registry.unregister(securityIdentity.getPrincipal().getName());
    }

    @OnTextMessage(codec = MessageDTODecoder.class)
    public void processAsync(Record message) {
        if (message instanceof GamePlayersMessageDTO) {
            if(! ((GamePlayersMessageDTO)message).senderSecondaryId().equals(securityIdentity.getPrincipal().getName())) {
                throw new UnauthorizedException("Secondary IDs do not match");
            }

            replyReceivedEmitter.send((GamePlayersMessageDTO)message);
        }

        if (message instanceof GameCastVoteMessageDTO) {
            registerVoteEmitter.send(
                (GameCastVoteMessageDTO)message
            );
        }

        if (message instanceof GameCancelledMessageDTO) {
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
