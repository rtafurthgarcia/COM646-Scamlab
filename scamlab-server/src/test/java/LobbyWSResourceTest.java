import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import model.dto.GameDto;
import model.dto.AuthenticationDTO.GetNewPlayerDTO;
import model.dto.GameDto.WaitingLobbyGameAssignmentMessageDTO;
import model.dto.GameDto.WaitingLobbyReasonForWaitingMessageDTO;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
@QuarkusTest
public class LobbyWSResourceTest {
    private static final LinkedBlockingDeque<Record> MESSAGES = new LinkedBlockingDeque<>();

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
            .as(GetNewPlayerDTO.class);

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

        for (int i = 0; i < 2; i++) {
            var message = MESSAGES.poll(10, TimeUnit.SECONDS);
            if (message instanceof WaitingLobbyGameAssignmentMessageDTO) {
                assertNotNull(message);
            }

            if (message instanceof WaitingLobbyReasonForWaitingMessageDTO) {
                var reasonForWaiting = (WaitingLobbyReasonForWaitingMessageDTO) message;
                assertEquals(GameDto.WSReasonForWaiting.NOT_ENOUGH_PLAYERS, reasonForWaiting.reasons());
            }

            //assertNotNull(message);
        }
    }

    @WebSocketClient(path = "/ws/lobby")
    public static class ClientEndpoint {
        @OnTextMessage
        void onMessage(Record message, WebSocketClientConnection connection) {
            MESSAGES.add(message);
        }
    }
}