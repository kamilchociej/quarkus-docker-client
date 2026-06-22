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

/** Mirrors docker-java's StatsCmdIT. */
@QuarkusTest
public class StatsCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // StatsCmdIT#testStatsNoStreaming
    @Test
    public void testStatsContainer() {
        String id = createAndStartContainer("top");
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/stats")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("read", notNullValue());
        } finally {
            removeContainer(id);
        }
    }
}
