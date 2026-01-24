package dev.ivfrost.hydro_backend;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import dev.ivfrost.hydro_backend.users.UserTokenProvider;
import dev.ivfrost.hydro_backend.users.UserRegisterRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HydroApiApplicationTests {

  @Autowired
  private UserTokenProvider userTokenProvider;

  @LocalServerPort
  private int port;

  @BeforeAll
  static void setupBaseUri() {
    RestAssured.baseURI = "http://localhost";
  }

  // Test user registration endpoint
  @Test
  void testUserRegistration() {
    RestAssured.port = port;
    UserRegisterRequest request = new UserRegisterRequest(
        "test@mail.com",
        "testuser",
        "Test User",
        "password123",
        "es");

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/v1/users")
        .then()
        .assertThat()
        .statusCode(HttpStatus.CREATED.value())
        .body("message", equalTo("User registered successfully"))
        .body("details", notNullValue())
        .body("details", hasSize(5))
        .body("details[0].type", equalTo("RECOVERY_CODE"))
        .body("details[0].value", notNullValue())
        .body("details.findAll { it.type == 'RECOVERY_CODE' }.size()", equalTo(5));
  }

  @Test
  void contextLoads() {
  }
}
