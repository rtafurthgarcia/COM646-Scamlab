package helper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

/*
    Application scoped conversation manager.
    Basically, keep a count of the involved players who started the game before the timeout ran out. 
 */
@ApplicationScoped
public class VoteToStartRegistry {

    // Map player's secondary ID to conversation ID
    private final ConcurrentMap<Long, Long> VoteMap = new ConcurrentHashMap<>();

    /**
     * Registers a player's secondary ID with the corresponding connection ID.
     *
     * @param playerId The player's primary identifier.
     * @param conversationId The conversation's primary identifier.
     */
    public void register(Long playerId, Long conversationId) {
        VoteMap.put(playerId, conversationId);
    }

    /**
     * Retrieves the connection ID associated with the given player's secondary ID.
     *
     * @param playerId The player's primary identifier.
     * @return The corresponding conversation's primary identifier., or null if not found.
     */
    public Boolean hasVoted(Long playerId) {
        return VoteMap.containsKey(playerId);
    }

    /**
     * Removes the mapping for the given player's secondary ID.
     *
     * @param playerId The player's primary identifier.
     */
    public void unregister(Long playerId) {
        VoteMap.remove(playerId);
    }
}
