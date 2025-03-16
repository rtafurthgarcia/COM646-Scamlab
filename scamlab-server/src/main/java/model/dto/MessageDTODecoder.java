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
            case NOTIFY_REASON_FOR_WAITING -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyReasonForWaitingMessageDTO.class);
            case NOTIFY_START_MENU_STATISTICS -> 
                objectMapper.readValue(json, GameDTO.StartMenuStatisticsMessageDTO.class);
            case STRATEGY_ASSIGNED -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyAssignedStrategyMessageDTO.class);
            case READY_TO_START -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyReadyToStartMessageDTO.class);
            case VOTE_TO_START -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyVoteToStartMessageDTO.class);
            case VOTE_ACKNOWLEDGED -> 
                objectMapper.readValue(json, GameDTO.VoteAcknowledgedMessageDTO.class);
            case GAME_STARTING -> 
                objectMapper.readValue(json, GameDTO.WaitingLobbyGameStartingMessageDTO.class);
            case GAME_CANCELLED -> 
                objectMapper.readValue(json, GameDTO.GameGameCancelledMessageDTO.class);
            case CALL_TO_VOTE -> 
                objectMapper.readValue(json, GameDTO.GameCallToVoteMessageDTO.class);
            case CAST_VOTE -> 
                objectMapper.readValue(json, GameDTO.GameCastVoteMessageDTO.class);
            case GAME_FINISHED -> 
                objectMapper.readValue(json, GameDTO.GameFinishedMessageDTO.class);
            default -> throw new JsonException(json);
            };
        } catch (JsonProcessingException e) {
            Log.error(e);
            return null;
        }
    }
}