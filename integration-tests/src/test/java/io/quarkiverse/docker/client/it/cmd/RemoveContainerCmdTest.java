package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's RemoveContainerCmdImplIT. */
@QuarkusTest
public class RemoveContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // RemoveContainerCmdImplIT#removeContainer
    @Test
    public void testRemoveContainer() {
        String id = createAndStartContainer("true");
        given().post("/docker-container/" + id + "/wait").then().statusCode(200);

        given().delete("/docker-container/" + id).then().statusCode(204);

        given()
                .when()
                .get("/docker-container")
                .then()
                .statusCode(200)
                .body("find { it.Id == '" + id + "' }", nullValue());
    }

    // RemoveContainerCmdImplIT#removeNonExistingContainer
    @Test
    public void testRemoveNonExistingContainer() {
        given().delete("/docker-container/non-existing").then().statusCode(404);
    }
}
