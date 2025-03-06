import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import model.dto.AuthenticationDto.GetNewPlayerDto;
import service.AuthenticationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;


@QuarkusTest
public class AuthenticationResourceTest {

    @Inject
    AuthenticationService service; 

    // Corresponds to the first bit of S1 in the architectural documentation
    @Test
    public void testRegistrationSystem() {
        // register
        var player = given()
            .when().get("/api/players/new")
            .then()
            .statusCode(201)
            .and()
            .extract()
            .as(GetNewPlayerDto.class);
        
        var playerFromDb = service.findUserBySecondaryId(UUID.fromString(player.secondaryId()));

        assertEquals(player.systemRole(), playerFromDb.getSystemRole().toString());
        assertNotNull(player.jwtToken());

        // re-register
        given()
            .when().get("/api/players/new")
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
          .when().get("/api/players/new")
          .then()
          .statusCode(201)
          .and()
          .extract()
          .as(GetNewPlayerDto.class);
      
        playerFromDb = service.findUserBySecondaryId(UUID.fromString(player.secondaryId()));

        assertEquals(player.systemRole(), playerFromDb.getSystemRole().toString());
        assertNotNull(player.jwtToken());
    }
}