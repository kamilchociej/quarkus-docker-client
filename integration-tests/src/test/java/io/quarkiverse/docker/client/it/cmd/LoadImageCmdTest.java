package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's LoadImageCmdIT. */
@QuarkusTest
public class LoadImageCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // LoadImageCmdIT#loadImageFromTar : save then load an image round-trip
    @Test
    public void testLoadImage() {
        given()
                .when()
                .queryParam("name", "busybox")
                .post("/docker-image/load")
                .then()
                .statusCode(204);
    }
}
