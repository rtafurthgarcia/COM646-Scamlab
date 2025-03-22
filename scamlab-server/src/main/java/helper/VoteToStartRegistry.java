package helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.arc.Lock;
import io.quarkus.arc.Lock.Type;
import jakarta.enterprise.context.ApplicationScoped;

/*
    Application scoped conversation manager.
    Basically, keep a count of the involved players who started the game before the timeout ran out. 
 */
@ApplicationScoped
@Lock(value = Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
public class VoteToStartRegistry {

    // Map player's secondary ID to conversation ID
    private final Map<Long, Long> VoteMap = Collections.synchronizedMap(new HashMap<Long, Long>());

    /**
     * Registers a player's ID with the corresponding conversation ID.
     *
     * @param playerId The player's primary identifier.
     * @param conversationId The conversation's primary identifier.
     */
    public void register(Long playerId, Long conversationId) {
        VoteMap.put(playerId, conversationId);
    }

    /**
     * Retrieves the conversation ID associated with the given player's ID.
     *
     * @param playerId The player's primary identifier.
     * @return The corresponding conversation's primary identifier., or null if not found.
     */
    public Boolean hasVoted(Long playerId) {
        return VoteMap.containsKey(playerId);
    }

    /**
     * Removes the mapping for the given player's ID.
     *
     * @param playerId The player's primary identifier.
     */
    public void unregister(Long playerId) {
        VoteMap.remove(playerId);
    }
}
