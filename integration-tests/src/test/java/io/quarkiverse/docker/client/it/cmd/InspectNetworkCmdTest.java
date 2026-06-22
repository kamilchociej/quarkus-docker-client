package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's InspectNetworkCmdIT. */
@QuarkusTest
public class InspectNetworkCmdTest {

    // InspectNetworkCmdIT#inspectNetwork : inspect the built-in bridge network
    @Test
    public void testInspectNetwork() {
        given()
                .when()
                .get("/docker-network/bridge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Name", equalTo("bridge"))
                .body("Driver", equalTo("bridge"))
                .body("Scope", equalTo("local"));
    }
}
