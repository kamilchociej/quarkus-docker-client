package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies the network commands exposed by {@link NetworkResource}, mirroring
 * docker-java's CreateNetworkCmdIT, InspectNetworkCmdIT, ListNetworksCmdIT,
 * RemoveNetworkCmdIT, ConnectToNetworkCmdIT and DisconnectFromNetworkCmdIT.
 */
@QuarkusTest
public class NetworkResourceTest {

    private static final String IMAGE = "busybox:latest";

    private String createNetwork(String name) {
        return given()
                .when()
                .post("/docker-network/" + name)
                .then()
                .statusCode(200)
                .body("Id", not(emptyOrNullString()))
                .extract()
                .path("Id");
    }

    private void removeNetwork(String id) {
        try {
            given().delete("/docker-network/" + id);
        } catch (Exception ignored) {
        }
    }

    // CreateNetworkCmdIT#createNetwork : created network has bridge driver
    @Test
    public void testCreateNetwork() {
        String id = createNetwork("createNetwork-" + System.nanoTime());
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("Driver", equalTo("bridge"));
        } finally {
            removeNetwork(id);
        }
    }

    // CreateNetworkCmdIT#createNetworkWithIpamConfig
    @Test
    public void testCreateNetworkWithIpam() {
        String name = "networkIpam-" + System.nanoTime();
        String subnet = "10.67.79.0/24";
        String id = given()
                .when()
                .queryParam("subnet", subnet)
                .post("/docker-network/" + name + "/ipam")
                .then()
                .statusCode(200)
                .extract()
                .path("Id");
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .body("Driver", equalTo("bridge"))
                    .body("IPAM.Config[0].Subnet", equalTo(subnet));
        } finally {
            removeNetwork(id);
        }
    }

    // CreateNetworkCmdIT#createNetworkWithLabel
    @Test
    public void testCreateNetworkWithLabel() {
        String name = "createNetworkWithLabel-" + System.nanoTime();
        String id = given()
                .when()
                .post("/docker-network/" + name + "/label")
                .then()
                .statusCode(200)
                .extract()
                .path("Id");
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .body("Labels.'com.example.usage'", equalTo("test"));
        } finally {
            removeNetwork(id);
        }
    }

    // CreateNetworkCmdIT#createAttachableNetwork
    @Test
    public void testCreateAttachableNetwork() {
        String name = "createAttachableNetwork-" + System.nanoTime();
        String id = given()
                .when()
                .post("/docker-network/" + name + "/attachable")
                .then()
                .statusCode(200)
                .extract()
                .path("Id");
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .body("Attachable", equalTo(true));
        } finally {
            removeNetwork(id);
        }
    }

    // ListNetworksCmdIT#listNetworks : the built-in bridge network is present
    @Test
    public void testListNetworks() {
        given()
                .when()
                .get("/docker-network")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("find { it.Name == 'bridge' }.Scope", equalTo("local"))
                .body("find { it.Name == 'bridge' }.Driver", equalTo("bridge"))
                .body("find { it.Name == 'bridge' }.IPAM.Driver", equalTo("default"));
    }

    // InspectNetworkCmdIT#inspectNetwork : inspect the built-in bridge network
    @Test
    public void testInspectNetwork() {
        given()
                .when()
                .get("/docker-network/bridge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Name", equalTo("bridge"))
                .body("Driver", equalTo("bridge"))
                .body("Scope", equalTo("local"));
    }

    // RemoveNetworkCmdIT#removeNetwork : removed network no longer appears in the list
    @Test
    public void testRemoveNetwork() {
        String id = createNetwork("test-network-" + System.nanoTime());

        given()
                .when()
                .delete("/docker-network/" + id)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/docker-network")
                .then()
                .statusCode(200)
                .body("find { it.Id == '" + id + "' }", nullValue());
    }

    // RemoveNetworkCmdIT#removeNonExistingContainer
    @Test
    public void testRemoveNonExistingNetwork() {
        given()
                .when()
                .delete("/docker-network/non-existing")
                .then()
                .statusCode(404);
    }

    // ConnectToNetworkCmdIT#connectToNetwork
    @Test
    public void testConnectToNetwork() {
        String containerId = startContainer();
        String networkId = createNetwork("connectToNetwork-" + System.nanoTime());
        try {
            given()
                    .when()
                    .post("/docker-network/" + networkId + "/connect/" + containerId)
                    .then()
                    .statusCode(204);

            given()
                    .when()
                    .get("/docker-network/" + networkId)
                    .then()
                    .statusCode(200)
                    .body("Containers", hasKey(containerId));
        } finally {
            removeContainer(containerId);
            removeNetwork(networkId);
        }
    }

    // DisconnectFromNetworkCmdIT#disconnectFromNetwork
    @Test
    public void testDisconnectFromNetwork() {
        String containerId = startContainer();
        String networkId = createNetwork("disconnectNetwork-" + System.nanoTime());
        try {
            given().post("/docker-network/" + networkId + "/connect/" + containerId).then().statusCode(204);
            given().get("/docker-network/" + networkId).then().body("Containers", hasKey(containerId));

            given()
                    .when()
                    .post("/docker-network/" + networkId + "/disconnect/" + containerId)
                    .then()
                    .statusCode(204);

            given()
                    .when()
                    .get("/docker-network/" + networkId)
                    .then()
                    .statusCode(200)
                    .body("Containers", not(hasKey(containerId)));
        } finally {
            removeNetwork(networkId);
            removeContainer(containerId);
        }
    }

    private String startContainer() {
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
}
