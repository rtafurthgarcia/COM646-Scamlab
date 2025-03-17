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
import jakarta.resource.NotSupportedException;
import model.dto.MessageDTODecoder;
import model.dto.GameDTO.GameGameCancelledMessageDTO;
import model.dto.GameDTO.GamePlayersMessageDTO;
import model.dto.GameDTO.LeaveRequestDTO;
import model.dto.GameDTO.WaitingLobbyAssignedStrategyMessageDTO;
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
    Emitter<LeaveRequestDTO> leaveEmitter;

    @OnOpen
    public void onOpen() {
        Log.info("Player " + securityIdentity.getPrincipal().getName() + "joined conversation: " + connection.id());
        registry.register(securityIdentity.getPrincipal().getName(), connection.id());
    }

    @Incoming("send-reply")
    public Uni<Void> sendReply(WaitingLobbyAssignedStrategyMessageDTO message) throws NotSupportedException {
        throw new NotSupportedException();
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

    @OnTextMessage(codec = MessageDTODecoder.class)
    public void processAsync(Record message) {
        if (message instanceof GamePlayersMessageDTO) {
            var playerId = UUID.fromString(securityIdentity.getPrincipal().getName());

            /*registerStartGameEmitter.send(
                new VoteStartRequestDTO(
                    playerId, 
                    conversationId
                )
            );*/
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
