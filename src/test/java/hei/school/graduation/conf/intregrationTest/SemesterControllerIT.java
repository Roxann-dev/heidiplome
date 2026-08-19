package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.Semester;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
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

class SemesterControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String studentToken;
  private String teacherToken;
  private UUID promotionId;

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

    PromotionEntity promotion =
        promotionRepository.save(
            PromotionEntity.builder().label("Promotion 2023").entryYear(2023).build());
    promotionId = promotion.getId();

    semesterRepository.save(
        SemesterEntity.builder().promotion(promotion).number(1).cursusYear(1).build());
    semesterRepository.save(
        SemesterEntity.builder().promotion(promotion).number(2).cursusYear(1).build());
  }

  @AfterEach
  void tearDown() {
    semesterRepository.deleteAll();
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

  @Test
  void admin_can_list_semesters_by_promotion() {
    ResponseEntity<Semester[]> response =
        restTemplate.exchange(
            "/promotions/" + promotionId + "/semestres",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            Semester[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void student_cannot_list_semesters() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotionId + "/semestres",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_list_semesters() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotionId + "/semestres",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void admin_listing_returns_empty_array_when_promotion_has_no_semesters() {
    PromotionEntity emptyPromotion =
        promotionRepository.save(
            PromotionEntity.builder().label("Promotion 2024").entryYear(2024).build());

    ResponseEntity<Semester[]> response =
        restTemplate.exchange(
            "/promotions/" + emptyPromotion.getId() + "/semestres",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            Semester[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void admin_listing_returns_not_found_when_promotion_does_not_exist() {
    UUID unknownPromotionId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + unknownPromotionId + "/semestres",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void unauthenticated_cannot_list_semesters() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotionId + "/semestres",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
