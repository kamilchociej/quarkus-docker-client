package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createNetwork;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's RemoveNetworkCmdIT. */
@QuarkusTest
public class RemoveNetworkCmdTest {

    // RemoveNetworkCmdIT#removeNetwork : removed network no longer appears in the list
    @Test
    public void testRemoveNetwork() {
        String id = createNetwork("test-network-" + System.nanoTime());

        given()
                .when()
                .delete("/docker-network/" + id)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/docker-network")
                .then()
                .statusCode(200)
                .body("find { it.Id == '" + id + "' }", nullValue());
    }

    // RemoveNetworkCmdIT#removeNonExistingContainer
    @Test
    public void testRemoveNonExistingNetwork() {
        given()
                .when()
                .delete("/docker-network/non-existing")
                .then()
                .statusCode(404);
    }
}
