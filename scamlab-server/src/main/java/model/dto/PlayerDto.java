package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public abstract class PlayerDto {
    @RegisterForReflection
    public static record GetNewPlayerDto(
        String secondaryId, String systemRole, String jwtToken
    ) {
    }
}
