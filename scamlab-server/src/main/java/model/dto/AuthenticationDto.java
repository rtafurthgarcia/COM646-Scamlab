package model.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

public abstract class AuthenticationDTO {
    @RegisterForReflection
    public static record GetNewPlayerDTO(
        String secondaryId, String systemRole, String jwtToken
    ) {
    }
}
