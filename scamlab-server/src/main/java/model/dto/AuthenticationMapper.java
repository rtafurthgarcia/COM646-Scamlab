package model.dto;

import jakarta.enterprise.context.ApplicationScoped;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import model.entity.Player;

@ApplicationScoped
public class AuthenticationMapper {
    public GetNewPlayerDto toGetNewPlayerDto(Player player) {
        return new GetNewPlayerDto(player.getSecondaryId().toString(), player.getSystemRole().toString(), player.getToken());
    }
}
