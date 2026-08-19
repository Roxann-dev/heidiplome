package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String existingEmail;

  @BeforeEach
  void setUpRestTemplate() {
    restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  @BeforeEach
  void setUp() {
    existingEmail = "student+" + UUID.randomUUID() + "@school.mg";
    registerUser(existingEmail, UserRole.STUDENT, "P@ssw0rd123");
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  private void registerUser(String email, UserRole role, String rawPassword) {
    UserEntity user =
        UserEntity.builder()
            .reference("REF-" + UUID.randomUUID())
            .lastName("Test")
            .firstName("User")
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .role(role)
            .build();
    userRepository.save(user);
  }

  @Test
  void login_returns_token_when_credentials_are_valid() {
    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(existingEmail, "P@ssw0rd123"), LoginResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().accessToken()).isNotBlank();
    assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
  }

  @Test
  void login_returns_unauthorized_when_password_is_wrong() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(existingEmail, "WrongPassword1"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void login_returns_unauthorized_when_email_does_not_exist() {
    String unknownEmail = "unknown+" + UUID.randomUUID() + "@school.mg";

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(unknownEmail, "P@ssw0rd123"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void login_returns_bad_request_when_email_is_blank() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest("", "P@ssw0rd123"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void login_returns_bad_request_when_email_is_malformed() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest("not-an-email", "P@ssw0rd123"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void login_returns_bad_request_when_password_is_blank() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(existingEmail, ""), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void different_users_get_different_tokens() {
    String otherEmail = "other+" + UUID.randomUUID() + "@school.mg";
    registerUser(otherEmail, UserRole.STUDENT, "P@ssw0rd123");

    ResponseEntity<LoginResponse> response1 =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(existingEmail, "P@ssw0rd123"), LoginResponse.class);
    ResponseEntity<LoginResponse> response2 =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(otherEmail, "P@ssw0rd123"), LoginResponse.class);

    Assertions.assertNotNull(response1.getBody());
    Assertions.assertNotNull(response2.getBody());
    assertThat(response1.getBody().accessToken()).isNotEqualTo(response2.getBody().accessToken());
  }
}
