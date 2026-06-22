package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies the exec commands exposed by {@link ExecResource}, mirroring
 * docker-java's ExecCreateCmdImplIT, ExecStartCmdIT, InspectExecCmdIT and
 * ResizeExecCmdIT.
 */
@QuarkusTest
public class ExecResourceTest {

    private static final String IMAGE = "busybox:latest";

    private String startContainer() {
        given().queryParam("image", IMAGE).post("/docker-image/pull").then().statusCode(204);
        String id = given()
                .when()
                .queryParam("image", IMAGE)
                .queryParam("cmd", "sleep,9999")
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        given().post("/docker-container/" + id + "/start").then().statusCode(204);
        return id;
    }

    private void removeContainer(String id) {
        try {
            given().queryParam("force", true).delete("/docker-container/" + id);
        } catch (Exception ignored) {
        }
    }

    private String execCreate(String containerId, String cmd) {
        return given()
                .when()
                .queryParam("cmd", cmd)
                .post("/docker-exec/" + containerId + "/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    // ExecCreateCmdImplIT#execCreateTest
    @Test
    public void testExecCreate() {
        String containerId = startContainer();
        try {
            String execId = execCreate(containerId, "touch,file.log");
            org.junit.jupiter.api.Assertions.assertFalse(execId.isEmpty());
        } finally {
            removeContainer(containerId);
        }
    }

    // ExecStartCmdIT#execStart : exec creates a file inside the container
    @Test
    public void testExecStart() {
        String containerId = startContainer();
        try {
            String touchExec = execCreate(containerId, "touch,/execStartTest.log");
            given().post("/docker-exec/" + touchExec + "/start").then().statusCode(204);

            // verify the file now exists via a second exec (exit code 0)
            String checkExec = execCreate(containerId, "test,-e,/execStartTest.log");
            given().post("/docker-exec/" + checkExec + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + checkExec + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("ExitCode", equalTo(0));
        } finally {
            removeContainer(containerId);
        }
    }

    // InspectExecCmdIT#inspectExec : exit codes reflect command success/failure
    @Test
    public void testInspectExec() {
        String containerId = startContainer();
        try {
            // file does not exist yet -> exit code 1
            String check1 = execCreate(containerId, "test,-e,/marker");
            given().post("/docker-exec/" + check1 + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + check1 + "/inspect")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("Running", equalTo(false))
                    .body("ExitCode", equalTo(1));

            // create the file -> exit code 0
            String touch = execCreate(containerId, "touch,/marker");
            given().post("/docker-exec/" + touch + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + touch + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Running", equalTo(false))
                    .body("ExitCode", equalTo(0));

            // file now exists -> exit code 0
            String check2 = execCreate(containerId, "test,-e,/marker");
            given().post("/docker-exec/" + check2 + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + check2 + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("ExitCode", equalTo(0));
        } finally {
            removeContainer(containerId);
        }
    }

    // ResizeExecCmdIT#resizeExecInstanceTtyTest
    @Test
    public void testResizeExec() {
        String containerId = startContainer();
        try {
            given()
                    .when()
                    .post("/docker-exec/" + containerId + "/resize-scenario")
                    .then()
                    .statusCode(200)
                    .body("completed", equalTo(true));
        } finally {
            removeContainer(containerId);
        }
    }
}
