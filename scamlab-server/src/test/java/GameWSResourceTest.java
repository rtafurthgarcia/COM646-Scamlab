import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import model.dto.AuthenticationDto.GetNewPlayerDto;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GameWSResourceTest {
    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/ws/games")
    URI uri;

    ClientEndpointConfig clientConfig;

    @BeforeEach
    public void getAuthenticationToken() {
        // Register new player and get token
        var player = given()
                .when().get("/api/players/new")
                .then()
                .statusCode(201)
                .extract()
                .as(GetNewPlayerDto.class);

        var token = player.jwtToken();

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
    public void testWaitingLobbyStatistics() throws Exception {

        // Connect to WebSocket endpoint
        try (Session session = ContainerProvider.getWebSocketContainer()
                .connectToServer(new Client(), clientConfig, uri)) {

            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
        }
    }

    @ClientEndpoint
    public static class Client extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            MESSAGES.add("CONNECT");
            session.addMessageHandler(String.class,
                    (MessageHandler.Whole<String>) MESSAGES::add);
            session.getAsyncRemote().sendText("_ready_");
        }
    }
}