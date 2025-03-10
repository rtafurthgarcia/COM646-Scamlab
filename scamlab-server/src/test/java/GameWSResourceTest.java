import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import model.dto.GameDto.WaitingLobbyAssignedStrategyMessageDto;
import model.dto.GameDto.WaitingLobbyStatisticsMessageDto;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReaderFactory;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import helper.DefaultKeyValues;

//@TestInstance(Lifecycle.PER_CLASS)
@QuarkusTest
public class GameWSResourceTest {
    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/")
    URI uri;

    String token;

    @Inject
    WebSocketConnector<ClientEndpoint> connector; 

    @Inject
    EntityManager entityManager; 

    @BeforeEach
    public void getAuthenticationToken() throws InterruptedException, UnsupportedEncodingException {
        // Register new player and get token
        var player = given()
            .when().get("/api/players/new")
            .then()
            .statusCode(201)
            .extract()
            .as(GetNewPlayerDto.class);

        token = player.jwtToken();

        // Build the subprotocol header value following the expected format.
        var quarkusHeader = "quarkus-http-upgrade#Authorization#Bearer " + token;
        // Encode the value to avoid any URI encoding issues.
        var encodedHeader = URLEncoder.encode(quarkusHeader, StandardCharsets.UTF_8.toString());

        connector
            .baseUri(uri) 
            .addSubprotocol("bearer-token-carrier")
            .addSubprotocol(encodedHeader);
    }

    // Corresponds to the last bit of S1 in the architectural documentation
    @Test
    //@Transactional
    public void testJoiningGame() throws InterruptedException, JsonMappingException, JsonProcessingException {
        // Join a new game
        given()
            .when()
            .header("Authorization", "Bearer " + token)  // Add Authorization Header
            .get("/api/games/join")
            .then()
            .statusCode(200);

        connector.connectAndAwait();

        var mapper = new ObjectMapper();
        var message = mapper.readValue(MESSAGES.poll(10, TimeUnit.SECONDS), WaitingLobbyStatisticsMessageDto.class);
        assertEquals(0, message.ongoingGamesCount());
        assertEquals(0, message.waitingPlayerCount());
        assertEquals(3, message.maxOngoingGamesCount());

        // // Make sure game has been created
        // var results = entityManager.createQuery(
        //     """
        //         SELECT COUNT(c) FROM Conversation c
        //         WHERE c.currentState.id = :state
        //             """, Object[].class)
        //     .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
        //     .getResultList();

        // assertEquals(1, results.size());

        var assignedStrategy = mapper.readValue(MESSAGES.poll(10, TimeUnit.SECONDS), WaitingLobbyAssignedStrategyMessageDto.class);
        assertNotNull(assignedStrategy, "Bruh");
        //assertEquals(1, assignedStrategy.waitingPlayerCount());
        //assertEquals(3, assignedStrategy.maxOngoingGamesCount());

        message = mapper.readValue(MESSAGES.poll(10, TimeUnit.SECONDS), WaitingLobbyStatisticsMessageDto.class);
        assertEquals(0, message.ongoingGamesCount());
        assertEquals(1, message.waitingPlayerCount());
        assertEquals(3, message.maxOngoingGamesCount());
    }

    @WebSocketClient(path = "/ws/games")
    public static class ClientEndpoint {
        @OnTextMessage
        void onMessage(String message, WebSocketClientConnection connection) {
            MESSAGES.add(message);
        }
    }
}