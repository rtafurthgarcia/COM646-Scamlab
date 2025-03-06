package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class GameDto {
    @RegisterForReflection
    public static record WaitingLobbyStatisticsDto(
        Integer waitingPlayerCount, Integer ongoingGamesCount, Integer maxOngoingGamesCount
    ) {
    }
}
