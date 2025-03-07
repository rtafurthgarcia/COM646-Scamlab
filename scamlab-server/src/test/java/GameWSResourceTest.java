import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.Session;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import model.entity.Conversation;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import helper.DefaultKeyValues;

//@TestInstance(Lifecycle.PER_CLASS)
@QuarkusTest
public class GameWSResourceTest {
    private static final LinkedBlockingDeque<JsonObject> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/ws/games")
    URI uri;

    ClientEndpointConfig clientConfig;

    String token;

    @Inject
    EntityManager entityManager; 

    @BeforeEach
    public void getAuthenticationToken() throws InterruptedException {
        // Register new player and get token
        var player = given()
                .when().get("/api/players/new")
                .then()
                .statusCode(201)
                .extract()
                .as(GetNewPlayerDto.class);

        token = player.jwtToken();

        // Create subprotocol with encoded authorization header
        var headerProtocol = "quarkus-http-upgrade#Authorization#Bearer " + token;
        var encodedProtocol = URLEncoder.encode(headerProtocol, StandardCharsets.UTF_8)
                .replace("+", "%20"); // Proper URI encoding

        // Configure client with subprotocols
        clientConfig = ClientEndpointConfig.Builder.create()
                .preferredSubprotocols(List.of(
                        "bearer-token-carrier", // Our custom protocol
                        encodedProtocol // Quarkus header protocol
                ))
                .build();
    }

    // Corresponds to the last bit of S1 in the architectural documentation
    @Test
    public void testJoiningGame() throws Exception {

        // Join a new game
        given()
            .when()
            .header("Authorization", "Bearer " + token)  // Add Authorization Header
            .get("/api/games/join")
            .then()
            .statusCode(200);

        // Make sure game has been created
        var results = entityManager.createQuery(
            """
                SELECT c.id, s.id, COUNT(p) FROM Conversation c
                JOIN c.participants p
                JOIN c.strategy s
                WHERE c.currentState.id = :state
                GROUP BY c.id, s.id
                HAVING COUNT(p) < 2
                    """, Object[].class)
            .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
            .getResultList();

        assertEquals(results.size(), 1);

        // Connect to WebSocket endpoint
        try (Session session = ContainerProvider.getWebSocketContainer()
                .connectToServer(new Client(), clientConfig, uri)) {

            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
            //Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
        }
    }

    // A custom JSON decoder to convert incoming text messages to JsonObject
    public static class JsonDecoder implements Decoder.Text<JsonObject> {
        @Override
        public JsonObject decode(String s) throws DecodeException {
            try (StringReader reader = new StringReader(s);
                 JsonReader jsonReader = Json.createReader(reader)) {
                return jsonReader.readObject();
            } catch (Exception e) {
                throw new DecodeException(s, "Unable to decode JSON", e);
            }
        }

        @Override
        public boolean willDecode(String s) {
            return s != null && !s.isEmpty();
        }

        @Override
        public void init(EndpointConfig config) {}

        @Override
        public void destroy() {}
    }

    // Annotate the client endpoint with our JSON decoder.
    @ClientEndpoint(decoders = {JsonDecoder.class})
    public static class Client extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            // Immediately add a JSON message for connection confirmation.
            MESSAGES.add(Json.createObjectBuilder().add("status", "CONNECT").build());
            // Register a handler that will receive JSON messages
            session.addMessageHandler(JsonObject.class, (MessageHandler.Whole<JsonObject>) MESSAGES::add);
            // Send a ready signal to the server (if expected)
            session.getAsyncRemote().sendText("_ready_");
        }
    }
}