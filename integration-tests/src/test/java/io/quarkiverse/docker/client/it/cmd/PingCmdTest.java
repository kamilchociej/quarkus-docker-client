package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's PingCmdIT. */
@QuarkusTest
public class PingCmdTest {

    @Test
    public void testPing() {
        given()
                .when()
                .get("/docker-system/ping")
                .then()
                .statusCode(204);
    }
}
