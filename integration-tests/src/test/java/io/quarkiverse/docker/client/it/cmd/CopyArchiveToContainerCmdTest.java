package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's CopyArchiveToContainerCmdIT. */
@QuarkusTest
public class CopyArchiveToContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // CopyArchiveToContainerCmdIT#copyStreamToContainer : copy a file in, verify via copy-from
    @Test
    public void testCopyToContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            given()
                    .when()
                    .queryParam("remotePath", "/")
                    .queryParam("fileName", "testReadFile")
                    .queryParam("content", "hello")
                    .post("/docker-container/" + id + "/copy-to")
                    .then()
                    .statusCode(204);

            byte[] tar = given()
                    .when()
                    .queryParam("resource", "/testReadFile")
                    .get("/docker-container/" + id + "/copy-from")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asByteArray();
            Assertions.assertTrue(tar.length > 0);
        } finally {
            removeContainer(id);
        }
    }

    // CopyArchiveToContainerCmdIT#copyToNonExistingContainer
    @Test
    public void testCopyToNonExistingContainer() {
        given()
                .when()
                .queryParam("remotePath", "/")
                .queryParam("fileName", "f")
                .queryParam("content", "x")
                .post("/docker-container/non-existing/copy-to")
                .then()
                .statusCode(404);
    }
}
