package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import model.entity.Player.SystemRole;

public abstract class PlayerDto {
    @RegisterForReflection
    public static record GetNewPlayerDto(
        String secondaryId, SystemRole systemRole, long numberOfConnectedPlayers
    ) {
    }
}
