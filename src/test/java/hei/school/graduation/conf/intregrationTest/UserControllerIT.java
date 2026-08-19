package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.dto.UserCreateRequest;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.User;
import hei.school.graduation.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String studentToken;
  private String teacherToken;

  @BeforeEach
  void setUpRestTemplate() {
    restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  @BeforeEach
  void setUp() {
    String adminEmail = "admin+" + UUID.randomUUID() + "@school.mg";
    registerUser(adminEmail, UserRole.ADMIN);
    adminToken = login(adminEmail);

    String studentEmail = "student+" + UUID.randomUUID() + "@school.mg";
    registerUser(studentEmail, UserRole.STUDENT);
    studentToken = login(studentEmail);

    String teacherEmail = "teacher+" + UUID.randomUUID() + "@school.mg";
    registerUser(teacherEmail, UserRole.TEACHER);
    teacherToken = login(teacherEmail);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  private void registerUser(String email, UserRole role) {
    UserEntity user =
        UserEntity.builder()
            .reference("REF-" + UUID.randomUUID())
            .lastName("Test")
            .firstName("User")
            .email(email)
            .passwordHash(passwordEncoder.encode("P@ssw0rd123"))
            .role(role)
            .build();
    userRepository.save(user);
  }

  private String login(String email) {
    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(email, "P@ssw0rd123"), LoginResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody().accessToken();
  }

  private HttpEntity<Void> authenticatedGet(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private HttpEntity<UserCreateRequest> authenticated(UserCreateRequest body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  @Test
  void admin_can_list_all_users() {
    ResponseEntity<User[]> response =
        restTemplate.exchange("/users", HttpMethod.GET, authenticatedGet(adminToken), User[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(3);
  }

  @Test
  void student_cannot_list_all_users() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users", HttpMethod.GET, authenticatedGet(studentToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_list_all_users() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users", HttpMethod.GET, authenticatedGet(teacherToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_list_users() {
    ResponseEntity<String> response =
        restTemplate.exchange("/users", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void admin_can_filter_users_by_role() {
    ResponseEntity<User[]> response =
        restTemplate.exchange(
            "/users?role=STUDENT", HttpMethod.GET, authenticatedGet(adminToken), User[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].role()).isEqualTo(UserRole.STUDENT);
  }

  @Test
  void admin_can_create_user() {
    UserCreateRequest request =
        new UserCreateRequest(
            "REF-" + UUID.randomUUID(),
            "Rakoto",
            "Jean",
            "new+" + UUID.randomUUID() + "@school.mg",
            "P@ssw0rd123",
            UserRole.STUDENT);

    ResponseEntity<User> response =
        restTemplate.exchange(
            "/users", HttpMethod.POST, authenticated(request, adminToken), User.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().email()).isEqualTo(request.email());
    assertThat(response.getBody().role()).isEqualTo(UserRole.STUDENT);
  }

  @Test
  void create_user_with_duplicate_email_returns_conflict() {
    String duplicateEmail = "duplicate+" + UUID.randomUUID() + "@school.mg";
    registerUser(duplicateEmail, UserRole.STUDENT);

    UserCreateRequest request =
        new UserCreateRequest(
            "REF-" + UUID.randomUUID(),
            "Rakoto",
            "Jean",
            duplicateEmail,
            "P@ssw0rd123",
            UserRole.STUDENT);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users", HttpMethod.POST, authenticated(request, adminToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void student_cannot_create_user() {
    UserCreateRequest request =
        new UserCreateRequest(
            "REF-" + UUID.randomUUID(),
            "Rakoto",
            "Jean",
            "blocked+" + UUID.randomUUID() + "@school.mg",
            "P@ssw0rd123",
            UserRole.STUDENT);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users", HttpMethod.POST, authenticated(request, studentToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
