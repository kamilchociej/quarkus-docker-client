package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's CreateVolumeCmdIT. */
@QuarkusTest
public class CreateVolumeCmdTest {

    private static final String VOLUME_NAME = "volume1";

    @AfterEach
    public void cleanup() {
        CmdTestSupport.removeVolume(VOLUME_NAME);
    }

    // CreateVolumeCmdIT#createVolume
    @Test
    public void testCreateVolume() {
        given()
                .when()
                .post("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Name", equalTo(VOLUME_NAME))
                .body("Driver", equalTo("local"))
                .body("Labels.'is-timelord'", equalTo("yes"))
                .body("Mountpoint", containsString("/" + VOLUME_NAME + "/"));
    }

    // CreateVolumeCmdIT#createVolumeWithExistingName
    @Test
    public void testCreateVolumeWithExistingName() {
        String mountpoint1 = given()
                .when()
                .post("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(200)
                .extract()
                .path("Mountpoint");

        given()
                .when()
                .post("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(200)
                .body("Name", equalTo(VOLUME_NAME))
                .body("Mountpoint", equalTo(mountpoint1));
    }
}
