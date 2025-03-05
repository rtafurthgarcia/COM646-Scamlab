package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public class ConversationDto {
    @RegisterForReflection
    public static record WaitingLobbyStatisticsDto(
        Integer waitingPlayerCount, Integer usedLobbiesCount, Integer maxLobbiesCount
    ) {
    }
}
