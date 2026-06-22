package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's RemoveImageCmdIT. */
@QuarkusTest
public class RemoveImageCmdTest {

    // RemoveImageCmdIT#removeNonExistingImage
    @Test
    public void testRemoveNonExistingImage() {
        given()
                .when()
                .queryParam("name", "non-existing")
                .delete("/docker-image")
                .then()
                .statusCode(404);
    }
}
