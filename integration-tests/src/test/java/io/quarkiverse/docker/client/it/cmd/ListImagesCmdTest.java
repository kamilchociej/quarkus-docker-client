package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/** Mirrors docker-java's ListImagesCmdIT. */
@QuarkusTest
public class ListImagesCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
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
}
