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
import jakarta.inject.Inject;
import jakarta.resource.NotSupportedException;
import model.dto.MessageDTODecoder;
import model.dto.GameDTO.GameGameCancelledMessageDTO;
import model.dto.GameDTO.GamePlayersMessageDTO;
import model.dto.GameDTO.LeaveRequestInternalDTO;
import model.entity.TransitionReason;

@Authenticated
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
    @Channel("handle-player-leaving")
    @Broadcast
    Emitter<LeaveRequestInternalDTO> leaveEmitter;

    @Inject
    @Channel("reply-received")
    @Broadcast
    Emitter<GamePlayersMessageDTO> replyReceivedEmitter;

    @OnOpen
    public void onOpen() {
        Log.info("Player " + securityIdentity.getPrincipal().getName() + "joined conversation: " + connection.pathParam("conversationSecondaryId"));
        registry.register(securityIdentity.getPrincipal().getName(), connection.id());
    }

    @Incoming("send-reply")
    public Uni<Void> sendReply(GamePlayersMessageDTO message) {
        Log.info("Player " + message.receiverSecondaryId() + " is about to receive a message from Player " + message.senderSecondaryId());
        
        return connectionManager.findByConnectionId(
            registry.getConnectionId(message.receiverSecondaryId())
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
