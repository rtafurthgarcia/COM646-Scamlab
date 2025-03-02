package model.dto;

import jakarta.enterprise.context.ApplicationScoped;
import model.dto.PlayerDto.GetNewPlayerDto;
import model.entity.Player;

@ApplicationScoped
public class PlayerMapper {
    public GetNewPlayerDto toGetNewPlayerDto(Player player, long numberOfConnectedPlayers) {
        return new GetNewPlayerDto(player.getSecondaryId().toString(), player.getSystemRole(), player.getToken(), numberOfConnectedPlayers);
    }
}
