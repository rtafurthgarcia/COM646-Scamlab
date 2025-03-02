import io.quarkus.test.junit.QuarkusTest;
import model.dto.PlayerDto.GetNewPlayerDto;
import model.entity.Player.SystemRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;


@QuarkusTest
public class PlayerResourceTest {

    @Test
    public void testJoin() {
        GetNewPlayerDto player = given()
            .when().get("/api/players/join")
            .then()
            .statusCode(201)
            .and()
            .extract()
            .as(GetNewPlayerDto.class);
        
        assertEquals(player.systemRole(), SystemRole.ADMIN);
        assertNotNull(player.numberOfConnectedPlayers());
        assertNotNull(player.secondaryId());
    }
}