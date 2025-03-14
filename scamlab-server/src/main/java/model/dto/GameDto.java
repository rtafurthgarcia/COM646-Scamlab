package model.dto;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import io.quarkus.runtime.annotations.RegisterForReflection;
import model.entity.TransitionReason;

public class GameDTO {
    private static final AtomicLong counter = new AtomicLong(0);

    public static enum WSReasonForWaiting {
        NOT_ENOUGH_PLAYERS("Waiting on other players to join..."),
        ALL_LOBBIES_OCCUPIED("Waiting on a free lobby..."),
        START_CANCELLED_TIEMOUT("Players didnt start the game on time...");

        public final String message;

        private WSReasonForWaiting(String message) {
            this.message = message;
        }
    }

    // Helper method to get the next sequence number from the CDI bean.
    private static Long autoSequence() {
        return counter.incrementAndGet();
    }

    @RegisterForReflection
    public static record WaitingLobbyReasonForWaitingMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            List<String> reasons,
            Long sequence) implements MessageDTO {
        public WaitingLobbyReasonForWaitingMessageDTO(String playerSecondaryId, List<WSReasonForWaiting> reasons) {
            this(WSMessageType.NOTIFY_REASON_FOR_WAITING,
                    playerSecondaryId,
                    reasons.stream().map(r -> r.message).toList(),
                    autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record StartMenuStatisticsMessageDTO(
            WSMessageType type,
            int playersConnectedCount,
            Long sequence) implements MessageDTO {
        public StartMenuStatisticsMessageDTO(int playersConnectedCount) {
            this(WSMessageType.NOTIFY_START_MENU_STATISTICS, playersConnectedCount, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyAssignedStrategyMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            String conversationSecondaryId,
            String role,
            String script,
            String example,
            String strategy,
            String username,
            Long sequence) implements MessageDTO {
        public WaitingLobbyAssignedStrategyMessageDTO(
                String playerSecondaryId,
                String conversationSecondaryId,
                String role,
                String script,
                String example,
                String strategy,
                String username) {
            this(WSMessageType.STRATEGY_ASSIGNED,
                    playerSecondaryId,
                    conversationSecondaryId,
                    role,
                    script,
                    example,
                    strategy,
                    username,
                    autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyReadyToStartMessageDTO(
            WSMessageType type,
            Long voteTimeout,
            String playerSecondaryId,
            Long sequence) implements MessageDTO {
        public WaitingLobbyReadyToStartMessageDTO(Long voteTimeout, String playerSecondaryId) {
            this(WSMessageType.READY_TO_START, voteTimeout, playerSecondaryId, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyVoteToStartMessageDTO(
            WSMessageType type,
            String conversationSecondaryId,
            Long sequence) implements MessageDTO {
        public WaitingLobbyVoteToStartMessageDTO(String conversationSecondaryId) {
            this(WSMessageType.VOTE_TO_START, conversationSecondaryId, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record VoteAcknowledgedMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            Long sequence) implements MessageDTO {
        public VoteAcknowledgedMessageDTO(String playerSecondaryId) {
            this(WSMessageType.VOTE_ACKNOWLEDGED, playerSecondaryId, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyGameStartingMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            Long sequence) implements MessageDTO {
        public WaitingLobbyGameStartingMessageDTO(String playerSecondaryId) {
            this(WSMessageType.GAME_STARTING, playerSecondaryId, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record GameGameCancelledMessageDTO(
            WSMessageType type,
            Long sequence) implements MessageDTO {
        public GameGameCancelledMessageDTO() {
            this(WSMessageType.GAME_CANCELLED, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record GameCallToVoteMessageDTO(
            WSMessageType type,
            Long voteTimeout,
            Long sequence) implements MessageDTO {
        public GameCallToVoteMessageDTO(Long voteTimeout) {
            this(WSMessageType.CALL_TO_VOTE, voteTimeout, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record GameCastVoteMessageDTO(
            WSMessageType type,
            String playerSecondaryId,
            Long sequence) implements MessageDTO {
        public GameCastVoteMessageDTO(String playerSecondaryId) {
            this(WSMessageType.CAST_VOTE, playerSecondaryId, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    @RegisterForReflection
    public static record GameFinishedMessageDTO(
            WSMessageType type,
            Long sequence) implements MessageDTO {
        public GameFinishedMessageDTO() {
            this(WSMessageType.GAME_FINISHED, autoSequence());
        }

        @Override
        public WSMessageType getType() {
            return type;
        }

        @Override
        public Long getSequence() {
            return sequence;
        }
    }

    // The following request DTOs remain unchanged
    @RegisterForReflection
    public static record VoteStartRequestDTO(
            UUID player,
            UUID conversation) {
    }

    @RegisterForReflection
    public static record LeaveRequestDTO(
            UUID player,
            TransitionReason reason) {
    }
}
