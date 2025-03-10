package model.dto;

import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import model.entity.TransitionReason;

public class GameDto {
    @RegisterForReflection
    public static enum WSMessageType {
        NOTIFY_START_MENU_STATISTICS(1),
        NOTIFY_REASON_FOR_WAITING(2),
        STRATEGY_ASSIGNED(3),
        READY_TO_START(4),
        VOTE_TO_START(5),
        VOTE_ACKNOWLEDGED(6),
        GAME_STARTING(7),
        GAME_CANCELLED(8),
        CALL_TO_VOTE(9),
        CAST_VOTE(10),
        GAME_FINISHED(11);

        public final Long value;

        private WSMessageType(Integer value) {
            this.value = Integer.toUnsignedLong(value);
        }
    }

    public static enum WSReasonForWaiting{
        NOT_ENOUGH_PLAYERS("Waiting on other players to join..."),
        ALL_LOBBIES_OCCUPIED("Waiting on a free lobby..."),
        START_CANCELLED_TIEMOUT("Players didnt start the game on time...");

        public final String message;

        private WSReasonForWaiting(String message) {
            this.message = message;
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyReasonForWaitingMessageDto(
        WSMessageType type, String conversationSecondaryId, String message
    ) {
        public WaitingLobbyReasonForWaitingMessageDto(String conversationSecondaryId, WSReasonForWaiting reason) {
            this(WSMessageType.NOTIFY_REASON_FOR_WAITING, conversationSecondaryId, reason.message);
        }
    }

    @RegisterForReflection
    public static record StartMenuStatisticsMessageDto(
        WSMessageType type, int playersConnectedCount
    ) {
        public StartMenuStatisticsMessageDto(int playersConnectedCount) {
            this(WSMessageType.NOTIFY_START_MENU_STATISTICS, playersConnectedCount);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyAssignedStrategyMessageDto(
        WSMessageType type, 
        String playerSecondaryId, 
        String conversationSecondaryId, 
        String role, 
        String script, 
        String example, 
        String strategy, 
        String username
    ) {
        public WaitingLobbyAssignedStrategyMessageDto(
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
                username);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyReadyToStartMessageDto(
        WSMessageType type, Long voteTimeout, String playerSecondaryId
    ) {
        public WaitingLobbyReadyToStartMessageDto(Long voteTimeout, String playerSecondaryId) {
            this(WSMessageType.READY_TO_START, voteTimeout, playerSecondaryId);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyVoteToStartMessageDto(
        WSMessageType type, String conversationSecondaryId
    ) {
        public WaitingLobbyVoteToStartMessageDto(String conversationSecondaryId) {
            this(WSMessageType.VOTE_TO_START, conversationSecondaryId);
        }
    }

    @RegisterForReflection
    public static record VoteAcknowledgedMessageDto(
        WSMessageType type, String playerSecondaryId
    ) {
        public VoteAcknowledgedMessageDto(String playerSecondaryId) {
            this(WSMessageType.VOTE_ACKNOWLEDGED, playerSecondaryId);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyGameStartingMessageDto(
        WSMessageType type, String playerSecondaryId
    ) {
        public WaitingLobbyGameStartingMessageDto(String playerSecondaryId) {
            this(WSMessageType.GAME_STARTING, playerSecondaryId);
        }
    }

    @RegisterForReflection
    public static record GameGameCancelledMessageDto(
        WSMessageType type
    ) {
    }

    @RegisterForReflection
    public static record GameCallToVoteMessageDto(
        WSMessageType type, Long voteTimeout 
    ) {
    }

    @RegisterForReflection
    public static record GameCastVoteMessageDto(
        WSMessageType type, String playerSecondaryId
    ) {
    }

    @RegisterForReflection
    public static record GameFinishedMessageDto(
        WSMessageType type
    ) {
    }

    @RegisterForReflection
    public static record VoteStartRequestDto(
        UUID player,
        UUID conversation 
    ) {};

    @RegisterForReflection
    public static record CancellationRequestDto(
        UUID player,
        TransitionReason reason 
    ) {};
}
