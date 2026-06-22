package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies the system commands exposed by {@link SystemResource}, mirroring
 * docker-java's InfoCmdIT, VersionCmdIT, PingCmdIT and EventsCmdIT.
 */
@QuarkusTest
public class SystemResourceTest {

    // InfoCmdIT#infoTest
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

    // VersionCmdIT#version
    @Test
    public void testVersion() {
        given()
                .when()
                .get("/docker-system/version")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Version", not(emptyOrNullString()))
                .body("GoVersion", not(emptyOrNullString()))
                // docker version follows the major.minor.patch convention
                .body("Version.split('\\\\.').size()", greaterThanOrEqualTo(3));
    }

    // PingCmdIT#ping
    @Test
    public void testPing() {
        given()
                .when()
                .get("/docker-system/ping")
                .then()
                .statusCode(204);
    }

    // EventsCmdIT#testEventStreamTimeBound
    @Test
    public void testEvents() {
        given()
                .when()
                .get("/docker-system/events")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // generateEvents() produces pull/create/start/stop/destroy events
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.Action == 'start' }.size()", greaterThanOrEqualTo(1));
    }
}
