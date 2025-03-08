package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class GameDto {
    @RegisterForReflection
    public static enum WSMessageType {
        NOTIFY_START_MENU_STATISTICS(1),
        NOTIFY_WAITING_LOBBY_STATISTICS(2);

        public final Long value;

        private WSMessageType(Integer value) {
            this.value = Integer.toUnsignedLong(value);
        }
    }

    @RegisterForReflection
    public static record WaitingLobbyStatisticsMessageDto(
        WSMessageType type, Long waitingPlayerCount, Long ongoingGamesCount, Long maxOngoingGamesCount
    ) {
    }

    @RegisterForReflection
    public static record StartMenuStatisticsMessageDto(
        WSMessageType type, int numberOfPlayersConnected
    ) {
    }
}
