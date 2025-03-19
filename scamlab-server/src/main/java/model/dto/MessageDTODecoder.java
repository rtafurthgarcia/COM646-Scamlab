package model.dto;

import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.TextMessageCodec;
import jakarta.inject.Singleton;
import jakarta.json.JsonException;

@Singleton
public class MessageDTODecoder implements TextMessageCodec<Record> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(Type type) {
        // Allows selecting the right codec for the right type
        return type.equals(Record.class);
    }

    @Override
    public String encode(Record value) {
        // Serialization
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public Record decode(Type type, String json) {
        // Deserialization
        try {
        JsonNode root = objectMapper.readTree(json);
        JsonNode typeNode = root.get("type");
        WSMessageType messageType = WSMessageType.fromValue(typeNode.asLong());

        return switch (messageType) {
            case VOTE_TO_START -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyVoteToStartMessageDTO.class);
            case GAME_STARTING -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyGameStartingMessageDTO.class);
            case GAME_CANCELLED -> 
                objectMapper.readValue(json, GameDTO.GameGameCancelledMessageDTO.class);
            case CAST_VOTE -> 
                objectMapper.readValue(json, GameDTO.GameCastVoteMessageDTO.class);
            case PLAYERS_MESSAGE -> 
                objectMapper.readValue(json, GameDTO.GamePlayersMessageDTO.class);
            case RECONCILE_STATE -> 
                objectMapper.readValue(json, GameDTO.GameReconcileStateMessageDTO.class);
            default -> throw new JsonException(json);
            };
        } catch (JsonProcessingException e) {
            Log.error(e);
            return null;
        }
    }
}