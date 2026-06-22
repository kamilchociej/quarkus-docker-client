package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createNetwork;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeNetwork;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Mirrors docker-java's ConnectToNetworkCmdIT. */
@QuarkusTest
public class ConnectToNetworkCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    // ConnectToNetworkCmdIT#connectToNetwork
    @Test
    public void connectToNetwork() {
        String containerId = createAndStartContainer("sleep,9999");
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
}
