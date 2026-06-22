package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's CopyFileFromContainerCmdIT. */
@QuarkusTest
public class CopyFileFromContainerCmdTest {

    // CopyFileFromContainerCmdIT#copyFromNonExistingContainer
    @Test
    public void testCopyFileFromNonExistingContainer() {
        given().queryParam("resource", "/test").get("/docker-container/non-existing/copy-file-from").then()
                .statusCode(404);
    }
}
