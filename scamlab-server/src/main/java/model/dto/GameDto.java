package model.dto;

import java.util.List;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import model.entity.TransitionReason;

public class GameDTO {
    public static enum WSReasonForWaiting {
        NOT_ENOUGH_PLAYERS("Waiting on other player(s) to join..."),
        ALL_LOBBIES_OCCUPIED("Waiting on a free lobby..."),
        START_CANCELLED_TIEMOUT("Player(s) didn't start the game on time..."),
        OTHER_PLAYERS_LEFT("Some player(s) left..."),
        SYNCHRONISING("Please hold on a bit...");

        public final String message;

        private WSReasonForWaiting(String message) {
            this.message = message;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyReasonForWaitingMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            List<String> reasons) implements MessageDTO {
        public WaitingLobbyReasonForWaitingMessageDTO(String playerSecondaryId, List<WSReasonForWaiting> reasons) {
            this(WSMessageType.NOTIFY_REASON_FOR_WAITING,
                    playerSecondaryId,
                    reasons.stream().map(r -> r.message).toList());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record StartMenuStatisticsMessageDTO(
            WSMessageType type,
            int playersConnectedCount) implements MessageDTO {
        public StartMenuStatisticsMessageDTO(int playersConnectedCount) {
            this(WSMessageType.NOTIFY_START_MENU_STATISTICS, playersConnectedCount);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyGameAssignmentMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            String conversationSecondaryId,
            String role,
            String script,
            String example,
            String strategy,
            String username) implements MessageDTO {
        public WaitingLobbyGameAssignmentMessageDTO(
                String playerSecondaryId,
                String conversationSecondaryId,
                String role,
                String script,
                String example,
                String strategy,
                String username) {
            this(WSMessageType.GAME_ASSIGNED,
                    playerSecondaryId,
                    conversationSecondaryId,
                    role,
                    script,
                    example,
                    strategy,
                    username);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyReadyToStartMessageDTO(
            WSMessageType type,
            Long voteTimeout,
            String playerSecondaryId) implements MessageDTO {
        public WaitingLobbyReadyToStartMessageDTO(Long voteTimeout, String playerSecondaryId) {
            this(WSMessageType.READY_TO_START, voteTimeout, playerSecondaryId);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyVoteToStartMessageDTO(
            WSMessageType type,
            String conversationSecondaryId) implements MessageDTO {
        public WaitingLobbyVoteToStartMessageDTO(String conversationSecondaryId) {
            this(WSMessageType.VOTE_TO_START, conversationSecondaryId);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record VoteAcknowledgedMessageDTO(
            WSMessageType type,
            String playerSecondaryId) implements MessageDTO {
        public VoteAcknowledgedMessageDTO(String playerSecondaryId) {
            this(WSMessageType.VOTE_ACKNOWLEDGED, playerSecondaryId);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyGameStartingMessageDTO(
            WSMessageType type,
            String playerSecondaryId) implements MessageDTO {
        public WaitingLobbyGameStartingMessageDTO(String playerSecondaryId) {
            this(WSMessageType.GAME_STARTING, playerSecondaryId);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record GameGameCancelledMessageDTO(
            WSMessageType type) implements MessageDTO {
        public GameGameCancelledMessageDTO() {
            this(WSMessageType.GAME_CANCELLED);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record GameCallToVoteMessageDTO(
            WSMessageType type,
            Long voteTimeout) implements MessageDTO {
        public GameCallToVoteMessageDTO(Long voteTimeout) {
            this(WSMessageType.CALL_TO_VOTE, voteTimeout);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record GameCastVoteMessageDTO(
            WSMessageType type,
            String playerSecondaryId) implements MessageDTO {
        public GameCastVoteMessageDTO(String playerSecondaryId) {
            this(WSMessageType.CAST_VOTE, playerSecondaryId);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record GameFinishedMessageDTO(
            WSMessageType type) implements MessageDTO {
        public GameFinishedMessageDTO() {
            this(WSMessageType.GAME_FINISHED);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    @RegisterForReflection
    public static record GamePlayersMessageDTO(
            WSMessageType type,
            String senderSecondaryId,
            String senderUsername,
            String receiverSecondaryId,
            String text,
            String imagePath) implements MessageDTO {
        public GamePlayersMessageDTO(String playerSecondaryId, String username, String text, String imagePath) {
            this(WSMessageType.PLAYERS_MESSAGE, playerSecondaryId, null, username, text, imagePath);
        }

        public GamePlayersMessageDTO(String playerSecondaryId, String username, String receiverSecondaryId, String text, String imagePath) {
            this(WSMessageType.PLAYERS_MESSAGE, playerSecondaryId, receiverSecondaryId, username, text, imagePath);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }

    // The following request DTOs remain unchanged
    @RegisterForReflection
    public static record VoteStartRequestInternalDTO(
            UUID player,
            UUID conversation) {
    }

    @RegisterForReflection
    public static record LeaveRequestInternalDTO(
            UUID player,
            TransitionReason reason) {
    }

    @RegisterForReflection
    public static record GameReconcileStateMessageDTO(
            WSMessageType type,
            String conversationSecondaryId, 
            Long state) implements MessageDTO {
        public GameReconcileStateMessageDTO(String conversationSecondaryId) {
            this(WSMessageType.RECONCILE_STATE, conversationSecondaryId, null);
        }

        public GameReconcileStateMessageDTO(String conversationSecondaryId, Long state) {
            this(WSMessageType.RECONCILE_STATE, conversationSecondaryId, state);
        }

        @Override
        public WSMessageType getType() {
            return type;
        }
    }
}
