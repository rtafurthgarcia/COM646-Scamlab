package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class GameDto {
    @RegisterForReflection
    public static enum WSMessageType {
        NOTIFY_START_MENU_STATISTICS(1),
        NOTIFY_WAITING_LOBBY_STATISTICS(2),
        NOTIFY_ASSIGNED_STRATEGY(3),
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

    @RegisterForReflection
    public static record WaitingLobbyStatisticsMessageDto(
        WSMessageType type, Long waitingPlayerCount, Long ongoingGamesCount, Long maxOngoingGamesCount
    ) {
        public WaitingLobbyStatisticsMessageDto(Long waitingPlayerCount, Long ongoingGamesCount, Long maxOngoingGamesCount) {
            this(WSMessageType.NOTIFY_WAITING_LOBBY_STATISTICS, waitingPlayerCount, ongoingGamesCount, maxOngoingGamesCount);
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
        WSMessageType type, String role, String script, String example, String strategy, String username, String conversationSecondaryId
    ) {
        public WaitingLobbyAssignedStrategyMessageDto(
            String role, 
            String script, 
            String example, 
            String strategy, 
            String username, 
            String conversationSecondaryId) {
                this(WSMessageType.NOTIFY_ASSIGNED_STRATEGY, role, script, example, strategy, username, conversationSecondaryId);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyReadyToStartMessageDto(
        WSMessageType type, Long voteTimeout
    ) {
        public WaitingLobbyReadyToStartMessageDto(Long voteTimeout) {
            this(WSMessageType.READY_TO_START, voteTimeout);
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
    public static record WaitingLobbyVoteAcknowledgedMessageDto(
        WSMessageType type
    ) {
        public WaitingLobbyVoteAcknowledgedMessageDto() {
            this(WSMessageType.VOTE_ACKNOWLEDGED);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyGameStartingMessageDto(
        WSMessageType type
    ) {
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
}
