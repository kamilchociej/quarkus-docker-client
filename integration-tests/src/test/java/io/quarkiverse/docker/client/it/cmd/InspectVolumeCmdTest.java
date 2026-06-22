package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's InspectVolumeCmdIT. */
@QuarkusTest
public class InspectVolumeCmdTest {

    private static final String VOLUME_NAME = "volume1";

    @AfterEach
    public void cleanup() {
        CmdTestSupport.removeVolume(VOLUME_NAME);
    }

    // InspectVolumeCmdIT#inspectVolume
    @Test
    public void testInspectVolume() {
        given().post("/docker-volume/" + VOLUME_NAME).then().statusCode(200);

        given()
                .when()
                .get("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Name", equalTo(VOLUME_NAME))
                .body("Driver", equalTo("local"))
                .body("Labels.'is-timelord'", equalTo("yes"))
                .body("Mountpoint", containsString("/" + VOLUME_NAME + "/"));
    }

    // InspectVolumeCmdIT#inspectNonExistentVolume
    @Test
    public void testInspectNonExistentVolume() {
        given()
                .when()
                .get("/docker-volume/non-existing")
                .then()
                .statusCode(404);
    }
}
