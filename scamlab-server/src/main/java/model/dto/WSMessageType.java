package model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum WSMessageType {
    NOTIFY_START_MENU_STATISTICS(1),
    NOTIFY_REASON_FOR_WAITING(2),
    STRATEGY_ASSIGNED(3),
    READY_TO_START(4),
    VOTE_TO_START(5),
    VOTE_ACKNOWLEDGED(6),
    GAME_STARTING(7),
    GAME_CANCELLED(8),
    CALL_TO_VOTE(9),
    CAST_VOTE(10),
    GAME_FINISHED(11);

    public final Long value;

    private WSMessageType(Integer value) {
        this.value = Integer.toUnsignedLong(value);
    }

    @JsonValue
    public Long getValue() {
        return this.value;
    }

    @JsonCreator
    public static WSMessageType fromValue(Long value) {
        for (WSMessageType type : WSMessageType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WSMessageType value: " + value);
    }
}