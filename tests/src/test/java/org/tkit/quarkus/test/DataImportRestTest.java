package org.tkit.quarkus.test;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class DataImportRestTest extends AbstractTest {

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
