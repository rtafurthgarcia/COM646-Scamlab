package helper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

/*
    Application scoped websocket connection manager => allows to find the right match between connection id and player's id 
 */
@ApplicationScoped
public class PlayerConnectionRegistry {

    // Map player's secondary ID to connection ID
    private final ConcurrentMap<String, String> playerToConnectionMap = new ConcurrentHashMap<>();

    /**
     * Registers a player's secondary ID with the corresponding connection ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     * @param connectionId The WebSocket connection ID.
     */
    public void register(String playerSecondaryId, String connectionId) {
        playerToConnectionMap.put(playerSecondaryId, connectionId);
    }

    /**
     * Retrieves the connection ID associated with the given player's secondary ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     * @return The corresponding connection ID, or null if not found.
     */
    public String getConnectionId(String playerSecondaryId) {
        return playerToConnectionMap.get(playerSecondaryId);
    }

    /**
     * Removes the mapping for the given player's secondary ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     */
    public void unregister(String playerSecondaryId) {
        playerToConnectionMap.remove(playerSecondaryId);
    }
}