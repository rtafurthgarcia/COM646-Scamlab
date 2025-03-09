package model.dto;

import java.lang.reflect.Type;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.BinaryMessageCodec;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import model.dto.GameDto.GameCallToVoteMessageDto;
import model.dto.GameDto.GameCastVoteMessageDto;
import model.dto.GameDto.GameFinishedMessageDto;
import model.dto.GameDto.GameGameCancelledMessageDto;
import model.dto.GameDto.StartMenuStatisticsMessageDto;
import model.dto.GameDto.WSMessageType;
import model.dto.GameDto.WaitingLobbyGameStartingMessageDto;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import model.dto.GameDto.WaitingLobbyVoteAcknowledgedMessageDto;
import model.dto.GameDto.WaitingLobbyVoteToStartMessageDto;

@Singleton
public class GameMapper implements BinaryMessageCodec<Record> {
    private static final Set<Type> SUPPORTED_TYPES = Set.of(
        StartMenuStatisticsMessageDto.class,
        WaitingLobbyStatisticsMessageDto.class,
        WaitingLobbyVoteToStartMessageDto.class,
        WaitingLobbyVoteAcknowledgedMessageDto.class,
        WaitingLobbyGameStartingMessageDto.class,
        GameGameCancelledMessageDto.class,
        GameCallToVoteMessageDto.class,
        GameCastVoteMessageDto.class,
        GameFinishedMessageDto.class
    );

    ObjectMapper mapper;

    public GameMapper() {
        mapper = new ObjectMapper();
    }

    public static Record getWSMessage(JsonObject jsonObject) throws JsonParseException {
        if (jsonObject.containsKey("type")) {
            switch (WSMessageType.valueOf(jsonObject.getString("type"))) {
                case NOTIFY_START_MENU_STATISTICS:
                    return new GameDto.StartMenuStatisticsMessageDto(jsonObject.getInt("numberOfPlayersConnected"));
                case NOTIFY_WAITING_LOBBY_STATISTICS:
                    return new GameDto.WaitingLobbyStatisticsMessageDto(
                        Long.parseLong(jsonObject.getString("waitingPlayerCount")),
                        Long.parseLong(jsonObject.getString("ongoingGamesCount")),
                        Long.parseLong(jsonObject.getString("maxOngoingGamesCount")));
                case VOTE_TO_START:
                    return new GameDto.WaitingLobbyVoteToStartMessageDto();
                case VOTE_ACKNOWLEDGED:
                    return new GameDto.WaitingLobbyVoteAcknowledgedMessageDto();
                default:
                    throw new JsonParseException("JSON doesn't correspond to any recognised WS message type");
            }
        } else {
            throw new JsonParseException("JSON doesn't correspond to any recognised WS message");
        }
    }

    @Override
    public boolean supports(Type type) {
        return SUPPORTED_TYPES.contains(type);
    }
    

    @Override
    public Buffer encode(Record value) {
        try {
            return Buffer.buffer(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public Record decode(Type type, Buffer value) {
        throw new UnsupportedOperationException();
        //json.to
    }
}
