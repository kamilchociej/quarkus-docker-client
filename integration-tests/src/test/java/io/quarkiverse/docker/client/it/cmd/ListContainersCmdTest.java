package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's ListContainersCmdIT. */
@QuarkusTest
public class ListContainersCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // ListContainersCmdIT#testListContainers
    @Test
    public void testListContainers() {
        String id = createAndStartContainer("sleep,9999");
        try {
            given()
                    .when()
                    .get("/docker-container")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("find { it.Id == '" + id + "' }", notNullValue())
                    .body("find { it.Id == '" + id + "' }.Image", startsWith("busybox"));
        } finally {
            removeContainer(id);
        }
    }
}
