package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies the container commands exposed by {@link ContainerResource},
 * mirroring docker-java's container command integration tests
 * (CreateContainerCmdIT, StartContainerCmdIT, StopContainerCmdIT,
 * KillContainerCmdIT, RestartContainerCmdImplIT, PauseCmdIT, UnpauseCmdIT,
 * RenameContainerCmdIT, WaitContainerCmdIT, RemoveContainerCmdImplIT,
 * ListContainersCmdIT, InspectContainerCmdIT, ContainerDiffCmdIT,
 * ResizeContainerCmdIT, UpdateContainerCmdIT, LogContainerCmdIT, StatsCmdIT,
 * HealthCmdIT, AttachContainerCmdIT, CopyArchiveToContainerCmdIT,
 * CopyArchiveFromContainerCmdIT and CopyFileFromContainerCmdIT).
 */
@QuarkusTest
@Testcontainers
public class ContainerResourceTest {

    private static final String IMAGE = "busybox:latest";

    @Container
    public static GenericContainer<?> testContainer = new GenericContainer<>(IMAGE).withCommand("pwd");

    // ---- helpers -------------------------------------------------------------

    private String create(String cmd) {
        return given()
                .when()
                .queryParam("image", IMAGE)
                .queryParam("cmd", cmd)
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    private String createAndStart(String cmd) {
        String id = create(cmd);
        given().post("/docker-container/" + id + "/start").then().statusCode(204);
        return id;
    }

    private void remove(String id) {
        try {
            given().queryParam("force", true).delete("/docker-container/" + id);
        } catch (Exception ignored) {
        }
    }

    // CreateContainerCmdIT#createContainer
    @Test
    public void testCreateContainer() {
        String id = create("sleep,9999");
        try {
            org.junit.jupiter.api.Assertions.assertFalse(id.isEmpty());
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("Id", equalTo(id))
                    .body("Config.Image", equalTo(IMAGE));
        } finally {
            remove(id);
        }
    }

    // CreateContainerCmdIT : create with name, env and labels
    @Test
    public void testCreateContainerWithNameEnvAndLabels() {
        String name = "createTest-" + System.nanoTime();
        String id = given()
                .when()
                .queryParam("image", IMAGE)
                .queryParam("cmd", "env")
                .queryParam("name", name)
                .queryParam("env", "FOO=bar")
                .queryParam("label", "com.example=test")
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Name", equalTo("/" + name))
                    .body("Config.Env", hasItem("FOO=bar"))
                    .body("Config.Labels.'com.example'", equalTo("test"));
        } finally {
            remove(id);
        }
    }

    // CreateContainerCmdIT : creating from a missing image fails
    @Test
    public void testCreateContainerNonExistingImage() {
        given()
                .when()
                .queryParam("image", "non-existing-image-xyz:latest")
                .post("/docker-container/create")
                .then()
                .statusCode(404);
    }

    // StartContainerCmdIT#startContainer
    @Test
    public void testStartContainer() {
        String id = createAndStart("top");
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(true));
        } finally {
            remove(id);
        }
    }

    // StartContainerCmdIT#testStartNonExistingContainer
    @Test
    public void testStartNonExistingContainer() {
        given().post("/docker-container/non-existing/start").then().statusCode(404);
    }

    // StopContainerCmdIT#testStopContainer
    @Test
    public void testStopContainer() {
        String id = createAndStart("sleep,9999");
        try {
            given().post("/docker-container/" + id + "/stop?timeout=2").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(false));
        } finally {
            remove(id);
        }
    }

    // StopContainerCmdIT#testStopNonExistingContainer
    @Test
    public void testStopNonExistingContainer() {
        given().post("/docker-container/non-existing/stop").then().statusCode(404);
    }

    // KillContainerCmdIT#killContainer
    @Test
    public void testKillContainer() {
        String id = createAndStart("sleep,9999");
        try {
            given().post("/docker-container/" + id + "/kill").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(false))
                    .body("State.ExitCode", not(equalTo(0)));
        } finally {
            remove(id);
        }
    }

    // KillContainerCmdIT#killNonExistingContainer
    @Test
    public void testKillNonExistingContainer() {
        given().post("/docker-container/non-existing/kill").then().statusCode(404);
    }

    // RestartContainerCmdImplIT#restartContainer
    @Test
    public void testRestartContainer() {
        String id = createAndStart("sleep,9999");
        try {
            String startedAt1 = given().get("/docker-container/" + id + "/inspect")
                    .then().statusCode(200).extract().path("State.StartedAt");

            given().post("/docker-container/" + id + "/restart?timeout=2").then().statusCode(204);

            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(true))
                    .body("State.StartedAt", not(equalTo(startedAt1)));
        } finally {
            remove(id);
        }
    }

    // RestartContainerCmdImplIT#restartNonExistingContainer
    @Test
    public void testRestartNonExistingContainer() {
        given().post("/docker-container/non-existing/restart").then().statusCode(404);
    }

    // PauseCmdIT#pauseRunningContainer + UnpauseCmdIT#unpausePausedContainer
    @Test
    public void testPauseAndUnpauseContainer() {
        String id = createAndStart("sleep,9999");
        try {
            given().post("/docker-container/" + id + "/pause").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Paused", equalTo(true));

            given().post("/docker-container/" + id + "/unpause").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Paused", equalTo(false));
        } finally {
            remove(id);
        }
    }

    // PauseCmdIT#pauseNonExistingContainer
    @Test
    public void testPauseNonExistingContainer() {
        given().post("/docker-container/non-existing/pause").then().statusCode(404);
    }

    // RenameContainerCmdIT#renameContainer
    @Test
    public void testRenameContainer() {
        String id = createAndStart("sleep,9999");
        try {
            String newName = "renamed-" + System.nanoTime();
            given().post("/docker-container/" + id + "/rename?name=" + newName).then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Name", equalTo("/" + newName));
        } finally {
            remove(id);
        }
    }

    // RenameContainerCmdIT#renameExistingContainer (non-existing)
    @Test
    public void testRenameNonExistingContainer() {
        given().post("/docker-container/non-existing/rename?name=foo").then().statusCode(404);
    }

    // WaitContainerCmdIT#testWaitContainer
    @Test
    public void testWaitContainer() {
        String id = createAndStart("true");
        try {
            given()
                    .when()
                    .post("/docker-container/" + id + "/wait")
                    .then()
                    .statusCode(200)
                    .body(equalTo("0"));
        } finally {
            remove(id);
        }
    }

    // WaitContainerCmdIT#testWaitNonExistingContainer
    @Test
    public void testWaitNonExistingContainer() {
        given().post("/docker-container/non-existing/wait").then().statusCode(404);
    }

    // RemoveContainerCmdImplIT#removeContainer
    @Test
    public void testRemoveContainer() {
        String id = createAndStart("true");
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

    // ListContainersCmdIT#testListContainers
    @Test
    public void testListContainers() {
        String id = createAndStart("sleep,9999");
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
            remove(id);
        }
    }

    // InspectContainerCmdIT#inspectContainer
    @Test
    public void testInspectContainer() {
        String id = create("top");
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Id", equalTo(id))
                    .body("RestartCount", equalTo(0));
        } finally {
            remove(id);
        }
    }

    // InspectContainerCmdIT#inspectNonExistingContainer
    @Test
    public void testInspectNonExistingContainer() {
        given().get("/docker-container/non-existing/inspect").then().statusCode(404);
    }

    // ContainerDiffCmdIT#testContainerDiff
    @Test
    public void testContainerDiff() {
        String id = createAndStart("touch,/test");
        try {
            given().post("/docker-container/" + id + "/wait").then().statusCode(200);
            given()
                    .when()
                    .get("/docker-container/" + id + "/diff")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("find { it.Path == '/test' }.Kind", equalTo(1));
        } finally {
            remove(id);
        }
    }

    // ContainerDiffCmdIT#testContainerDiffWithNonExistingContainer
    @Test
    public void testContainerDiffNonExisting() {
        given().get("/docker-container/non-existing/diff").then().statusCode(404);
    }

    // ResizeContainerCmdIT#resizeContainerTtyTest
    @Test
    public void testResizeContainer() {
        String id = given()
                .when()
                .queryParam("image", IMAGE)
                .queryParam("cmd", "sh,-c,until stty size | grep '30 120'; do : ; done")
                .queryParam("tty", true)
                .queryParam("stdinOpen", true)
                .queryParam("user", "root")
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        try {
            given().post("/docker-container/" + id + "/start").then().statusCode(204);
            given().post("/docker-container/" + id + "/resize?height=30&width=120").then().statusCode(204);
            given()
                    .when()
                    .post("/docker-container/" + id + "/wait")
                    .then()
                    .statusCode(200)
                    .body(equalTo("0"));
        } finally {
            remove(id);
        }
    }

    // UpdateContainerCmdIT#updateContainer
    @Test
    public void testUpdateContainer() {
        String id = createAndStart("sleep,9999");
        try {
            given().post("/docker-container/" + id + "/update").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("HostConfig.CpuShares", equalTo(512))
                    .body("HostConfig.CpuPeriod", equalTo(100000))
                    .body("HostConfig.CpuQuota", equalTo(50000))
                    .body("HostConfig.CpusetMems", equalTo("0"));
        } finally {
            remove(id);
        }
    }

    // LogContainerCmdIT#asyncMultipleLogContainer
    @Test
    public void testLogContainer() {
        String id = createAndStart("/bin/echo,hello world");
        try {
            given().post("/docker-container/" + id + "/wait").then().statusCode(200);
            given()
                    .when()
                    .get("/docker-container/" + id + "/logs")
                    .then()
                    .statusCode(200)
                    .body(containsString("hello world"));
        } finally {
            remove(id);
        }
    }

    // StatsCmdIT#testStatsNoStreaming
    @Test
    public void testStatsContainer() {
        String id = createAndStart("top");
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/stats")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("read", notNullValue());
        } finally {
            remove(id);
        }
    }

    // HealthCmdIT#healthiness
    @Test
    public void testHealthContainer() {
        String id = given()
                .when()
                .queryParam("image", IMAGE)
                .queryParam("healthcheck", true)
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        try {
            given().post("/docker-container/" + id + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/health")
                    .then()
                    .statusCode(200)
                    .body(equalTo("healthy"));
        } finally {
            remove(id);
        }
    }

    // CopyArchiveToContainerCmdIT#copyStreamToContainer + CopyArchiveFromContainerCmdIT#copyFromContainer
    @Test
    public void testCopyArchiveToAndFromContainer() {
        String id = createAndStart("sleep,9999");
        try {
            given()
                    .when()
                    .post("/docker-container/" + id + "/copy-to?remotePath=/&fileName=testReadFile&content=hello")
                    .then()
                    .statusCode(204);

            byte[] tar = given()
                    .when()
                    .get("/docker-container/" + id + "/copy-from?resource=/testReadFile")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asByteArray();
            org.junit.jupiter.api.Assertions.assertTrue(tar.length > 0);
        } finally {
            remove(id);
        }
    }

    // CopyArchiveToContainerCmdIT#copyToNonExistingContainer
    @Test
    public void testCopyToNonExistingContainer() {
        given()
                .when()
                .post("/docker-container/non-existing/copy-to?remotePath=/&fileName=f&content=x")
                .then()
                .statusCode(404);
    }

    // CopyArchiveFromContainerCmdIT#copyFromNonExistingContainer
    @Test
    public void testCopyFromNonExistingContainer() {
        given().get("/docker-container/non-existing/copy-from?resource=/test").then().statusCode(404);
    }

    // CopyFileFromContainerCmdIT#copyFromNonExistingContainer
    @Test
    public void testCopyFileFromNonExistingContainer() {
        given().get("/docker-container/non-existing/copy-file-from?resource=/test").then().statusCode(404);
    }

    // AttachContainerCmdIT#attachContainerWithoutTTY
    @Test
    public void testAttachContainer() {
        given()
                .when()
                .post("/docker-container/attach-scenario?snippet=hello world")
                .then()
                .statusCode(200)
                .body(containsString("hello world"));
    }
}
