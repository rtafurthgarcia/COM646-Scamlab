import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.Session;
import model.dto.AuthenticationDto.GetNewPlayerDto;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StartMenuWSResourceTest {
    private static final LinkedBlockingDeque<JsonObject> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/ws/start-menu")
    URI uri;

    final static JsonObject returnJson = Json.createObjectBuilder().add("numberOfPlayersConnected", 1).build();

    @Test
    public void testGetCurrentCountOfConnectedUsers() throws Exception {
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
        var clientConfig = ClientEndpointConfig.Builder.create()
            .preferredSubprotocols(List.of(
                "bearer-token-carrier",  // Our custom protocol
                encodedProtocol          // Quarkus header protocol
            ))
            .build();

        // Connect to WebSocket endpoint
        try (var session = ContainerProvider.getWebSocketContainer()
                .connectToServer(new Client(), clientConfig, uri)) {
            Assertions.assertEquals(returnJson, MESSAGES.poll(10, TimeUnit.SECONDS));
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
        @OnOpen
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(JsonObject.class, (MessageHandler.Whole<JsonObject>) MESSAGES::add);
        }

        @OnMessage
        public void onMessage(String message) {
            System.out.println("Received: " + message);
            try {
                // Assuming you have a similar ChatMessage record locally:
                //ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
                //MESSAGES.add(chatMessage.numberOfPlayersConnected());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}