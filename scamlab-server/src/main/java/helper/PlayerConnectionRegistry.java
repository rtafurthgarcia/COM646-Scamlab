package helper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.quarkus.arc.Lock;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/*
    Application scoped websocket connection manager => allows to find the right match between connection id and player's id 
 */
@ApplicationScoped
@Lock
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
        //Log.info("New player " + playerSecondaryId + " registered on WS endpoint " + connectionId);
    }

    /**
     * Retrieves the connection ID associated with the given player's secondary ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     * @return The corresponding connection ID, or null if not found.
     */
    @Lock(value = Lock.Type.READ, time = 1, unit = TimeUnit.SECONDS)
    public String getConnectionId(String playerSecondaryId) {
        //Log.info("Player " + playerSecondaryId + " looked up");
        return playerToConnectionMap.get(playerSecondaryId);
    }

    /**
     * Removes the mapping for the given player's secondary ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     */
    public void unregister(String playerSecondaryId) {
        //Log.info("Player " + playerSecondaryId + " unregistered");
        playerToConnectionMap.remove(playerSecondaryId);
    }
}