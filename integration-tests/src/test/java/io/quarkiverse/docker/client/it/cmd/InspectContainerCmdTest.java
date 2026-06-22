package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's InspectContainerCmdIT. */
@QuarkusTest
public class InspectContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // InspectContainerCmdIT#inspectContainer
    @Test
    public void testInspectContainer() {
        String id = createContainer("top");
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Id", equalTo(id))
                    .body("RestartCount", equalTo(0));
        } finally {
            removeContainer(id);
        }
    }

    // InspectContainerCmdIT#inspectNonExistingContainer
    @Test
    public void testInspectNonExistingContainer() {
        given().get("/docker-container/non-existing/inspect").then().statusCode(404);
    }
}
