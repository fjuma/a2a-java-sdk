package io.a2a.server.apps.spring;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.TaskQueryParams;
import io.a2a.util.Utils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Integration tests for Spring A2A server adapter.
 * Tests the REST endpoints and JSON-RPC functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet"
})
@Import(SpringTestConfiguration.class)
public class SpringA2AServerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void testGetAgentCard() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/.well-known/agent.json")
        .then()
            .statusCode(200)
            .body("name", notNullValue())
            .body("version", notNullValue())
            .body("capabilities", notNullValue());
    }

    @Test
    void testGetTaskNotFound() throws Exception {
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams("non-existent-task-id"));
        String requestJson = Utils.OBJECT_MAPPER.writeValueAsString(request);

        given()
            .contentType(ContentType.JSON)
            .body(requestJson)
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .body("error.code", equalTo(-32001)); // TaskNotFoundError code
    }

    @Test
    void testInvalidJsonRequest() {
        String invalidJson = "{ invalid json }";

        given()
            .contentType(ContentType.JSON)
            .body(invalidJson)
        .when()
            .post("/")
        .then()
            .statusCode(200)
            .body("error.code", equalTo(-32700)); // JSON Parse Error
    }

    @Test
    void testAuthenticatedExtendedAgentCardNotSupported() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/agent/authenticatedExtendedCard")
        .then()
            .statusCode(404);
    }
} 
