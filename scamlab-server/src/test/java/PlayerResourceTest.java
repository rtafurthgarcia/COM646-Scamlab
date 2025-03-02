import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import model.dto.PlayerDto.GetNewPlayerDto;
import repository.PlayerRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;


@QuarkusTest
public class PlayerResourceTest {

    @Inject
    PlayerRepository repository;

    @Test
    public void testRegistrationSystem() {
        // register
        var player = given()
            .when().get("/api/players/join")
            .then()
            .statusCode(201)
            .and()
            .extract()
            .as(GetNewPlayerDto.class);
        
        var playerFromDb = repository.find("secondaryId", UUID.fromString(player.secondaryId())).firstResult();

        assertEquals(player.systemRole(), playerFromDb.getSystemRole().toString());
        assertNotNull(player.numberOfConnectedPlayers());
        assertNotNull(player.jwtToken());

        // re-register
        given()
            .when().get("/api/players/join")
            .then()
            .statusCode(409);

        // then unregister
        given()
            .when()
            .header("Authorization", "Bearer " + player.jwtToken())  // Add Authorization Header
            .delete("/api/players/" + player.secondaryId())
            .then()
            .statusCode(205);

        // re-register
        player = given()
          .when().get("/api/players/join")
          .then()
          .statusCode(201)
          .and()
          .extract()
          .as(GetNewPlayerDto.class);
      
        playerFromDb = repository.find("secondaryId", UUID.fromString(player.secondaryId())).firstResult();

        assertEquals(player.systemRole(), playerFromDb.getSystemRole().toString());
        assertNotNull(player.numberOfConnectedPlayers());
        assertNotNull(player.jwtToken());
    }
}