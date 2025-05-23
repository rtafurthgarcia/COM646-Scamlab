import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import jakarta.inject.Inject;
import model.dto.AuthenticationDTO.GetNewPlayerDTO;
import model.dto.GameDto.StartMenuStatisticsMessageDTO;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class StartMenuWSResourceTest {
    private static final LinkedBlockingDeque<StartMenuStatisticsMessageDTO> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/")
    URI uri;

    @Inject
    WebSocketConnector<ClientEndpoint> connector; 

    @Test
    public void testGetCurrentCountOfConnectedUsers() throws Exception {
        // Register new player and get token
        var player = given()
            .when().get("/api/players/new")
            .then()
            .statusCode(201)
            .extract()
            .as(GetNewPlayerDTO.class);

        var token = player.jwtToken();

        // Build the subprotocol header value following the expected format.
        var quarkusHeader = "quarkus-http-upgrade#Authorization#Bearer " + token;
        // Encode the value to avoid any URI encoding issues.
        var encodedHeader = URLEncoder.encode(quarkusHeader, StandardCharsets.UTF_8.toString());

        connector
            .baseUri(uri) 
            .addSubprotocol("bearer-token-carrier")
            .addSubprotocol(encodedHeader)
            .connectAndAwait();

        var message = MESSAGES.poll(10, TimeUnit.SECONDS);
        assertTrue(message.playersConnectedCount() >= 1);
    }

    @WebSocketClient(path = "/ws/start-menu")
    public static class ClientEndpoint {
        @OnTextMessage
        void onMessage(StartMenuStatisticsMessageDTO message, WebSocketClientConnection connection) {
            MESSAGES.add(message);
        }
    }
}