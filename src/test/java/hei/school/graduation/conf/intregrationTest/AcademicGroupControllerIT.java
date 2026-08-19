package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.GroupCreateRequest;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.AcademicGroup;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.time.LocalDate;
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

class AcademicGroupControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private AcademicGroupRepository academicGroupRepository;
  @Autowired private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String studentToken;
  private String teacherToken;

  private UserEntity studentEntity;
  private SemesterEntity semester;

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
    studentEntity = registerUser(studentEmail, UserRole.STUDENT);
    studentToken = login(studentEmail);

    String teacherEmail = "teacher+" + UUID.randomUUID() + "@school.mg";
    registerUser(teacherEmail, UserRole.TEACHER);
    teacherToken = login(teacherEmail);

    PromotionEntity promotion =
        promotionRepository.save(
            PromotionEntity.builder()
                .label("Promotion " + UUID.randomUUID())
                .entryYear(Math.abs(UUID.randomUUID().hashCode()))
                .build());

    semester =
        semesterRepository.save(
            SemesterEntity.builder().promotion(promotion).number(1).cursusYear(1).build());
  }

  @AfterEach
  void tearDown() {
    studentGroupAssignmentRepository.deleteAll();
    academicGroupRepository.deleteAll();
    semesterRepository.deleteAll();
    promotionRepository.deleteAll();
    userRepository.deleteAll();
  }

  private UserEntity registerUser(String email, UserRole role) {
    UserEntity user =
        UserEntity.builder()
            .reference("REF-" + UUID.randomUUID())
            .lastName("Test")
            .firstName("User")
            .email(email)
            .passwordHash(passwordEncoder.encode("P@ssw0rd123"))
            .role(role)
            .build();
    return userRepository.save(user);
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

  private <T> HttpEntity<T> authenticated(T body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  private AcademicGroupEntity saveGroup(String reference, Parcours parcours) {
    return academicGroupRepository.save(
        AcademicGroupEntity.builder()
            .reference(reference)
            .parcours(parcours)
            .semester(semester)
            .build());
  }

  @Test
  void admin_can_list_groups_by_semester() {
    saveGroup("K1", null);

    ResponseEntity<AcademicGroup[]> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            AcademicGroup[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].reference()).isEqualTo("K1");
  }

  @Test
  void student_can_list_groups_by_semester() {
    saveGroup("K1", null);

    ResponseEntity<AcademicGroup[]> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            AcademicGroup[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void teacher_can_list_groups_by_semester() {
    saveGroup("K1", null);

    ResponseEntity<AcademicGroup[]> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            AcademicGroup[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void unauthenticated_cannot_list_groups_by_semester() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void admin_can_create_group() {
    GroupCreateRequest request = new GroupCreateRequest("K1-EL", Parcours.EL);

    ResponseEntity<AcademicGroup> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            AcademicGroup.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().reference()).isEqualTo("K1-EL");
    assertThat(response.getBody().parcours()).isEqualTo(Parcours.EL);
    assertThat(response.getBody().semesterId()).isEqualTo(semester.getId());
  }

  @Test
  void student_cannot_create_group() {
    GroupCreateRequest request = new GroupCreateRequest("K1-EL", Parcours.EL);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_create_group() {
    GroupCreateRequest request = new GroupCreateRequest("K1-EL", Parcours.EL);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_create_group() {
    GroupCreateRequest request = new GroupCreateRequest("K1-EL", Parcours.EL);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/semestres/" + semester.getId() + "/groups", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void create_group_with_blank_reference_returns_bad_request() {
    GroupCreateRequest request = new GroupCreateRequest("", null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_group_for_nonexistent_semester_returns_not_found() {
    GroupCreateRequest request = new GroupCreateRequest("K1", null);
    UUID unknownSemesterId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/semestres/" + unknownSemesterId + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_group_with_duplicate_reference_returns_conflict() {
    saveGroup("K1", null);
    GroupCreateRequest request = new GroupCreateRequest("K1", null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/semestres/" + semester.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void admin_can_list_students_of_group() {
    AcademicGroupEntity group = saveGroup("K1", null);
    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(studentEntity)
            .group(group)
            .semestre(semester)
            .dateDebut(LocalDate.now())
            .build());

    ResponseEntity<StudentGroupAssignment[]> response =
        restTemplate.exchange(
            "/groups/" + group.getId() + "/students",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            StudentGroupAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].studentId()).isEqualTo(studentEntity.getId());
  }

  @Test
  void student_cannot_list_students_of_group() {
    AcademicGroupEntity group = saveGroup("K1", null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + group.getId() + "/students",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_list_students_of_group() {
    AcademicGroupEntity group = saveGroup("K1", null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + group.getId() + "/students",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_list_students_of_group() {
    AcademicGroupEntity group = saveGroup("K1", null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + group.getId() + "/students",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void list_students_of_nonexistent_group_returns_not_found() {
    UUID unknownGroupId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/groups/" + unknownGroupId + "/students",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
