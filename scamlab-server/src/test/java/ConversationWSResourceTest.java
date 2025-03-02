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
public class ConversationWSResourceTest {

    @Inject
    PlayerRepository repository;

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/ws/conversation/start")
    URI uri;

   @Test
    public void testGetCurrentCountOfConnectedUsers() throws Exception {
        // register
        var player = given()
            .when().get("/api/players/new")
            .then()
            .statusCode(201)
            .and()
            .extract()
            .as(GetNewPlayerDto.class);

        var authenticationToken = player.jwtToken();

        // Create a custom configurator to add the Authorization header with the JWT token.
        ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put("Authorization", Collections.singletonList("Bearer " + authenticationToken));
            }
        };

        ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create()
            .configurator(configurator)
            .build();

        // Instantiate the client endpoint.
        Client clientEndpoint = new Client();

        // Connect using the custom configuration.
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(clientEndpoint, clientConfig, uri)) {
            // Your assertions or message handling can follow here.
            // Example: Check for the connection open message.
            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
        }
    }

    @ClientEndpoint
    public static class Client extends Endpoint {
    @Override
        public void onOpen(Session session, EndpointConfig config) {
            MESSAGES.add("CONNECT");
            // Add a message handler to capture incoming text messages.
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    MESSAGES.add(message);
                }
            });
            // Optionally send a ready message.
            session.getAsyncRemote().sendText("_ready_");
        }
    }
}