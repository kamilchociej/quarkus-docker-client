package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's InfoCmdIT. */
@QuarkusTest
public class InfoCmdTest {

    @Test
    public void testInfo() {
        given()
                .when()
                .get("/docker-system/info")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Containers", notNullValue())
                .body("Images", notNullValue())
                .body("NCPU", greaterThan(0));
    }
}
