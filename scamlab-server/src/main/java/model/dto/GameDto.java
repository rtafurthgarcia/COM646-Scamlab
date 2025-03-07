package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class GameDto {
    @RegisterForReflection
    public static enum WSMessageType {
        NOTIFY(1);

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
}
