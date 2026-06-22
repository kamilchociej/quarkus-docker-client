package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's RestartContainerCmdImplIT. */
@QuarkusTest
public class RestartContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // RestartContainerCmdImplIT#restartContainer
    @Test
    public void testRestartContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            String startedAt1 = given().get("/docker-container/" + id + "/inspect")
                    .then().statusCode(200).extract().path("State.StartedAt");

            given().queryParam("timeout", 2).post("/docker-container/" + id + "/restart").then().statusCode(204);

            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(true))
                    .body("State.StartedAt", not(equalTo(startedAt1)));
        } finally {
            removeContainer(id);
        }
    }

    // RestartContainerCmdImplIT#restartNonExistingContainer
    @Test
    public void testRestartNonExistingContainer() {
        given().post("/docker-container/non-existing/restart").then().statusCode(404);
    }
}
