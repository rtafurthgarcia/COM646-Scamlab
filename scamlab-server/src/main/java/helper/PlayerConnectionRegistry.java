package helper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.quarkus.arc.Lock;
import io.quarkus.arc.Lock.Type;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/*
    Application scoped websocket connection manager => allows to find the right match between connection id and player's id 
 */
@ApplicationScoped
@Lock(value = Type.WRITE, time = 1, unit = TimeUnit.SECONDS)
public class PlayerConnectionRegistry {

    // Map player's secondary ID to connection ID
    private final ConcurrentMap<String, String> playerToConnectionMap = new ConcurrentHashMap<String, String>();

    /**
     * Registers a player's secondary ID with the corresponding connection ID.
     *s
     * @param playerSecondaryId The player's secondary identifier.
     * @param connectionId The WebSocket connection ID.
     */
    public void register(String playerSecondaryId, String connectionId) {
        var oldValue = playerToConnectionMap.put(playerSecondaryId, connectionId);

        if (oldValue != null) {
            Log.info("Player " + playerSecondaryId + " successfully reconnected");
        }

        //Log.info("BEAN HASHCODE: " + hashCode());
        //Log.info("BEAN LIST: " + String.join(",", playerToConnectionMap.keySet().stream().toList()));
    }

    /**
     * Retrieves the connection ID associated with the given player's secondary ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     * @return The corresponding connection ID, or null if not found.
     */
    public String getConnectionId(String playerSecondaryId) {
        //Log.info("Player " + playersSecondaryId + " requested");
        //Log.info("BEAN HASHCODE: " + hashCode());
        //Log.info("BEAN LIST: " + String.join(",", playerToConnectionMap.keySet().stream().toList()));

        return playerToConnectionMap.get(playerSecondaryId);
    }

    /**
     * Removes the mapping for the given player's secondary ID.
     *
     * @param playerSecondaryId The player's secondary identifier.
     */
    public void unregister(String playerSecondaryId) {
        playerToConnectionMap.remove(playerSecondaryId);

        Log.info("Player " + playerSecondaryId + " erased from connection registry");
        //Log.info("BEAN HASHCODE: " + hashCode());
        //Log.info("BEAN LIST: " + String.join(",", playerToConnectionMap.keySet().stream().toList()));
    }
}