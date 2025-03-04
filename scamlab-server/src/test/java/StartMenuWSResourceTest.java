import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.OnOpen;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import model.dto.PlayerDto.GetNewPlayerDto;
import repository.PlayerRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StartMenuWSResourceTest {

    @Inject
    PlayerRepository repository;

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/ws/start-menu")
    URI uri;

     @Test
    public void testGetCurrentCountOfConnectedUsers() throws Exception {
        // Register new player and get token
        var player = given()
            .when().get("/api/players/new")
            .then()
            .statusCode(201)
            .extract()
            .as(GetNewPlayerDto.class);

        String token = player.jwtToken();

        // Create subprotocol with encoded authorization header
        String headerProtocol = "quarkus-http-upgrade#Authorization#Bearer " + token;
        String encodedProtocol = URLEncoder.encode(headerProtocol, StandardCharsets.UTF_8)
                                          .replace("+", "%20"); // Proper URI encoding

        // Configure client with subprotocols
        ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create()
            .preferredSubprotocols(List.of(
                "bearer-token-carrier",  // Our custom protocol
                encodedProtocol          // Quarkus header protocol
            ))
            .build();

        // Connect to WebSocket endpoint
        try (Session session = ContainerProvider.getWebSocketContainer()
                .connectToServer(new Client(), clientConfig, uri)) {
                
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