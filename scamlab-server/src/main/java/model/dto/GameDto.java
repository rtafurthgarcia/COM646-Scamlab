package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class GameDto {
    @RegisterForReflection
    public static enum WSMessageType {
        NOTIFY_START_MENU_STATISTICS(1),
        NOTIFY_WAITING_LOBBY_STATISTICS(2),
        VOTE_TO_START(3),
        VOTE_ACKNOWLEDGED(4),
        GAME_STARTING(5),
        GAME_CANCELLED(6),
        CALL_TO_VOTE(7),
        CAST_VOTE(8),
        GAME_FINISHED(9);

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
    public static record WaitingLobbyVoteToStartMessageDto(
        WSMessageType type
    ) {
        public WaitingLobbyVoteToStartMessageDto() {
            this(WSMessageType.VOTE_TO_START);
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
