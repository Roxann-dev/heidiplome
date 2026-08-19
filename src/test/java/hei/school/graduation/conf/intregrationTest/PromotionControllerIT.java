package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.Promotion;
import hei.school.graduation.repository.PromotionRepository;
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

class PromotionControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
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
    promotionRepository.deleteAll();
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
    Assertions.assertNotNull(response.getBody());
    return response.getBody().accessToken();
  }

  private HttpEntity<Void> authenticatedGet(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private PromotionEntity savePromotion(String label, int entryYear) {
    return promotionRepository.save(
        PromotionEntity.builder().label(label).entryYear(entryYear).build());
  }

  @Test
  void admin_can_list_all_promotions() {
    savePromotion("Promotion A", Math.abs(UUID.randomUUID().hashCode()));
    savePromotion("Promotion B", Math.abs(UUID.randomUUID().hashCode()));

    ResponseEntity<Promotion[]> response =
        restTemplate.exchange(
            "/promotions", HttpMethod.GET, authenticatedGet(adminToken), Promotion[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void admin_receives_empty_list_when_no_promotion_exists() {
    ResponseEntity<Promotion[]> response =
        restTemplate.exchange(
            "/promotions", HttpMethod.GET, authenticatedGet(adminToken), Promotion[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void student_cannot_list_promotions() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions", HttpMethod.GET, authenticatedGet(studentToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_list_promotions() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions", HttpMethod.GET, authenticatedGet(teacherToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_list_promotions() {
    ResponseEntity<String> response =
        restTemplate.exchange("/promotions", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void admin_can_get_promotion_by_id() {
    PromotionEntity promotion = savePromotion("Promotion 2023", 2023);

    ResponseEntity<Promotion> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId(),
            HttpMethod.GET,
            authenticatedGet(adminToken),
            Promotion.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().label()).isEqualTo("Promotion 2023");
    assertThat(response.getBody().entryYear()).isEqualTo(2023);
  }

  @Test
  void get_nonexistent_promotion_returns_not_found() {
    UUID unknownPromotionId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + unknownPromotionId,
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void student_cannot_get_promotion_by_id() {
    PromotionEntity promotion = savePromotion("Promotion 2023", 2023);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId(),
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_get_promotion_by_id() {
    PromotionEntity promotion = savePromotion("Promotion 2023", 2023);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId(),
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_get_promotion_by_id() {
    PromotionEntity promotion = savePromotion("Promotion 2023", 2023);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
