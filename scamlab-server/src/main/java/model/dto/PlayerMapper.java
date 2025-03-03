package model.dto;

import jakarta.enterprise.context.ApplicationScoped;
import model.dto.PlayerDto.GetNewPlayerDto;
import model.entity.Player;

@ApplicationScoped
public class PlayerMapper {
    public GetNewPlayerDto toGetNewPlayerDto(Player player) {
        return new GetNewPlayerDto(player.getSecondaryId().toString(), player.getSystemRole().toString(), player.getToken());
    }
}
