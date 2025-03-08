package model.dto;

import com.fasterxml.jackson.core.JsonParseException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import model.dto.GameDto.WSMessageType;

@ApplicationScoped
public class GameMapper {
    public static Record getWSMessage(JsonObject jsonObject) throws JsonParseException {
        if (jsonObject.containsKey("type")) {
            switch (WSMessageType.valueOf(jsonObject.getString("type"))) {
                case NOTIFY_START_MENU_STATISTICS:
                    return new GameDto.StartMenuStatisticsMessageDto(WSMessageType.NOTIFY_START_MENU_STATISTICS, jsonObject.getInt("numberOfPlayersConnected"));
                case NOTIFY_WAITING_LOBBY_STATISTICS:
                    return new GameDto.WaitingLobbyStatisticsMessageDto(
                        WSMessageType.NOTIFY_WAITING_LOBBY_STATISTICS, 
                        Long.parseLong(jsonObject.getString("waitingPlayerCount")),
                        Long.parseLong(jsonObject.getString("ongoingGamesCount")),
                        Long.parseLong(jsonObject.getString("maxOngoingGamesCount")));
                default:
                    throw new JsonParseException("JSON doesn't correspond to any recognised WS message type");
            }
        } else {
            throw new JsonParseException("JSON doesn't correspond to any recognised WS message");
        }
    }
}
