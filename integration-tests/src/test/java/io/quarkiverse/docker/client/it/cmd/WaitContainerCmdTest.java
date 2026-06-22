package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's WaitContainerCmdIT. */
@QuarkusTest
public class WaitContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // WaitContainerCmdIT#testWaitContainer
    @Test
    public void testWaitContainer() {
        String id = createAndStartContainer("true");
        try {
            given()
                    .when()
                    .post("/docker-container/" + id + "/wait")
                    .then()
                    .statusCode(200)
                    .body(equalTo("0"));
        } finally {
            removeContainer(id);
        }
    }

    // WaitContainerCmdIT#testWaitNonExistingContainer
    @Test
    public void testWaitNonExistingContainer() {
        given().post("/docker-container/non-existing/wait").then().statusCode(404);
    }
}
