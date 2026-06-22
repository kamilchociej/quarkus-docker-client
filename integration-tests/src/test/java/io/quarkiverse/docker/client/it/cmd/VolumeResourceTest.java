package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies the volume commands exposed by {@link VolumeResource}, mirroring
 * docker-java's CreateVolumeCmdIT, InspectVolumeCmdIT, ListVolumesCmdIT and
 * RemoveVolumeCmdIT.
 */
@QuarkusTest
public class VolumeResourceTest {

    private static final String VOLUME_NAME = "volume1";

    @AfterEach
    public void cleanup() {
        try {
            given().delete("/docker-volume/" + VOLUME_NAME);
        } catch (Exception e) {
            // ignore cleanup errors
        }
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

    // CreateVolumeCmdIT#createVolumeWithExistingName : re-creating returns the same mountpoint
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

    // ListVolumesCmdIT#listVolumes
    @Test
    public void testListVolumes() {
        given().post("/docker-volume/" + VOLUME_NAME).then().statusCode(200);

        given()
                .when()
                .get("/docker-volume")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Volumes.size()", greaterThanOrEqualTo(1))
                .body("Volumes.Name", hasItem(VOLUME_NAME));
    }

    // RemoveVolumeCmdIT#removeVolume : after removal the volume is gone (404)
    @Test
    public void testRemoveVolume() {
        given().post("/docker-volume/" + VOLUME_NAME).then().statusCode(200);

        given()
                .when()
                .delete("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(404);
    }
}
