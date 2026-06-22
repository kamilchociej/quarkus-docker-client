package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's EventsCmdIT. */
@QuarkusTest
public class EventsCmdTest {

    @Test
    public void testEvents() {
        given()
                .when()
                .get("/docker-system/events")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.Action == 'start' }.size()", greaterThanOrEqualTo(1));
    }
}
