package org.tkit.quarkus.importer;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class ImportDataRestTest extends AbstractTest {

    @Test
    public void testHealthCheck() {
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/health")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }


}
