import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import helper.DefaultKeyValues;

//@TestInstance(Lifecycle.PER_CLASS)
@QuarkusTest
public class GameWSResourceTest {
    private static final LinkedBlockingDeque<JsonObject> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/ws/games")
    URI uri;

    String token;

    @Inject
    EntityManager entityManager; 

    final static JsonObject statusJson = Json.createObjectBuilder().add("status", "CONNECT").build();

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
    }

    // Corresponds to the last bit of S1 in the architectural documentation
    @Test
    public void testJoiningGame() {
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
                SELECT COUNT(c) FROM Conversation c
                WHERE c.currentState.id = :state
                    """, Object[].class)
            .setParameter("state", DefaultKeyValues.StateValue.WAITING.value)
            .getResultList();

        assertEquals(1, results.size());

    }
}