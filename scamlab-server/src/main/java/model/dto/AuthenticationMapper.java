package model.dto;

import jakarta.enterprise.context.ApplicationScoped;
import model.dto.AuthenticationDTO.GetNewPlayerDTO;
import model.entity.Player;

@ApplicationScoped
public class AuthenticationMapper {
    public GetNewPlayerDTO toGetNewPlayerDTO(Player player) {
        return new GetNewPlayerDTO(player.getSecondaryId().toString(), player.getSystemRole().toString(), player.getToken());
    }
}
