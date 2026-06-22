package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies the image commands exposed by {@link ImageResource}, mirroring
 * docker-java's ListImagesCmdIT, PullImageCmdIT, RemoveImageCmdIT,
 * TagImageCmdIT, CommitCmdIT, SaveImageCmdIT, SaveImagesCmdIT, LoadImageCmdIT
 * and BuildImageCmdIT.
 */
@QuarkusTest
public class ImageResourceTest {

    private static final String BUSYBOX = "busybox:latest";

    @BeforeEach
    public void ensureBusybox() {
        given().queryParam("image", BUSYBOX).post("/docker-image/pull").then().statusCode(204);
    }

    private String startContainer(String cmd) {
        String id = given()
                .when()
                .queryParam("image", BUSYBOX)
                .queryParam("cmd", cmd)
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

    private void removeImage(String name) {
        try {
            given().queryParam("name", name).queryParam("force", true).delete("/docker-image");
        } catch (Exception ignored) {
        }
    }

    private io.restassured.response.ValidatableResponse inspectImage(String name) {
        return given().queryParam("name", name).when().get("/docker-image/inspect").then();
    }

    // ListImagesCmdIT#listImages
    @Test
    public void testListImages() {
        given()
                .when()
                .get("/docker-image")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThan(0))
                .body("[0].Id", not(emptyOrNullString()))
                .body("[0].Created", notNullValue())
                .body("[0].Size", notNullValue());
    }

    // PullImageCmdIT#testPullImage
    @Test
    public void testPullImage() {
        String image = "alpine:3.17";
        removeImage(image);

        given()
                .when()
                .queryParam("image", image)
                .post("/docker-image/pull")
                .then()
                .statusCode(204);

        inspectImage(image)
                .statusCode(200)
                .body("Id", not(emptyOrNullString()));
    }

    // RemoveImageCmdIT#removeNonExistingImage
    @Test
    public void testRemoveNonExistingImage() {
        given()
                .when()
                .queryParam("name", "non-existing")
                .delete("/docker-image")
                .then()
                .statusCode(404);
    }

    // TagImageCmdIT#tagImage
    @Test
    public void testTagImage() {
        String tag = String.valueOf(Math.abs(System.nanoTime()));
        String tagged = "docker-java/busybox:" + tag;
        try {
            given()
                    .when()
                    .queryParam("image", BUSYBOX)
                    .queryParam("repository", "docker-java/busybox")
                    .queryParam("tag", tag)
                    .post("/docker-image/tag")
                    .then()
                    .statusCode(204);

            inspectImage(tagged).statusCode(200);
        } finally {
            removeImage(tagged);
        }
    }

    // TagImageCmdIT#tagNonExistingImage
    @Test
    public void testTagNonExistingImage() {
        given()
                .when()
                .queryParam("image", "non-existing")
                .queryParam("repository", "docker-java/busybox")
                .queryParam("tag", "1")
                .post("/docker-image/tag")
                .then()
                .statusCode(404);
    }

    // CommitCmdIT#commit : the committed image's parent is the busybox image
    @Test
    public void testCommit() {
        String containerId = startContainer("touch,/test");
        String imageId = null;
        try {
            String busyboxId = inspectImage("busybox").statusCode(200).extract().path("Id");

            imageId = given()
                    .when()
                    .post("/docker-image/commit/" + containerId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            inspectImage(imageId).statusCode(200).body("Parent", equalTo(busyboxId));
        } finally {
            removeContainer(containerId);
            if (imageId != null) {
                removeImage(imageId);
            }
        }
    }

    // CommitCmdIT#commitWithLabels
    @Test
    public void testCommitWithLabels() {
        String containerId = startContainer("touch,/test");
        String imageId = null;
        try {
            given().post("/docker-container/" + containerId + "/wait").then().statusCode(200);

            imageId = given()
                    .when()
                    .post("/docker-image/commit/" + containerId + "/labels")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            inspectImage(imageId)
                    .statusCode(200)
                    .body("Config.Labels.label1", equalTo("abc"))
                    .body("Config.Labels.label2", equalTo("123"));
        } finally {
            removeContainer(containerId);
            if (imageId != null) {
                removeImage(imageId);
            }
        }
    }

    // CommitCmdIT#commitNonExistingContainer
    @Test
    public void testCommitNonExistingContainer() {
        given()
                .when()
                .post("/docker-image/commit/non-existent")
                .then()
                .statusCode(404);
    }

    // SaveImageCmdIT#saveImage : the exported tar stream is not empty
    @Test
    public void testSaveImage() {
        byte[] tar = given()
                .when()
                .queryParam("name", "busybox")
                .get("/docker-image/save")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(tar.length > 0);
    }

    // SaveImagesCmdIT#saveImagesWithNameAndTag
    @Test
    public void testSaveImages() {
        byte[] tar = given()
                .when()
                .queryParam("repository", "busybox")
                .queryParam("tag", "latest")
                .get("/docker-image/save-images")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(tar.length > 0);
    }

    // LoadImageCmdIT#loadImageFromTar : save then load an image round-trip
    @Test
    public void testLoadImage() {
        given()
                .when()
                .queryParam("name", "busybox")
                .post("/docker-image/load")
                .then()
                .statusCode(204);
    }

    // BuildImageCmdIT#labels : build an image carrying a label
    @Test
    public void testBuildImage() {
        String imageId = null;
        try {
            imageId = given()
                    .when()
                    .post("/docker-image/build")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            inspectImage(imageId).statusCode(200).body("Config.Labels.test", equalTo("abc"));
        } finally {
            if (imageId != null) {
                removeImage(imageId);
            }
        }
    }
}
