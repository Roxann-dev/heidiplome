package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.dto.StudentGroupAssignmentCreateRequest;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
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

class StudentGroupAssignmentControllerIT extends FacadeIT {

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

  private UserEntity student;
  private SemesterEntity semester;
  private AcademicGroupEntity group;

  @BeforeEach
  void setUpRestTemplate() {
    restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  @BeforeEach
  void setUp() {
    String adminEmail = "admin+" + UUID.randomUUID() + "@school.mg";
    registerUser(adminEmail, UserRole.ADMIN);
    adminToken = login(adminEmail);

    String teacherEmail = "teacher+" + UUID.randomUUID() + "@school.mg";
    registerUser(teacherEmail, UserRole.TEACHER);
    teacherToken = login(teacherEmail);

    String studentEmail = "student+" + UUID.randomUUID() + "@school.mg";
    student = registerUser(studentEmail, UserRole.STUDENT);
    studentToken = login(studentEmail);

    PromotionEntity promotion =
        promotionRepository.save(
            PromotionEntity.builder()
                .label("Promotion " + UUID.randomUUID())
                .entryYear(Math.abs(UUID.randomUUID().hashCode()))
                .build());

    semester =
        semesterRepository.save(
            SemesterEntity.builder().promotion(promotion).number(1).cursusYear(1).build());

    group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder()
                .reference("K1-" + UUID.randomUUID().toString().substring(0, 8))
                .parcours(Parcours.EL)
                .semester(semester)
                .build());
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

  @Test
  void admin_can_list_student_group_assignment_history() {
    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(student)
            .group(group)
            .semestre(semester)
            .dateDebut(LocalDate.of(2025, 9, 1))
            .dateFin(LocalDate.of(2026, 1, 31))
            .build());

    ResponseEntity<StudentGroupAssignment[]> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            StudentGroupAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].studentId()).isEqualTo(student.getId());
    assertThat(response.getBody()[0].groupId()).isEqualTo(group.getId());
  }

  @Test
  void unauthenticated_cannot_list_student_group_assignment_history() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void student_cannot_list_student_group_assignment_history() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_list_student_group_assignment_history() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void list_history_for_nonexistent_student_returns_not_found() {
    UUID unknownId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + unknownId + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void list_history_for_non_student_user_returns_bad_request() {
    UserEntity teacher =
        registerUser("teacher2+" + UUID.randomUUID() + "@school.mg", UserRole.TEACHER);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + teacher.getId() + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void list_history_returns_empty_list_when_no_assignments() {
    ResponseEntity<StudentGroupAssignment[]> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            StudentGroupAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void admin_can_assign_student_to_group() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<StudentGroupAssignment> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            StudentGroupAssignment.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().studentId()).isEqualTo(student.getId());
    assertThat(response.getBody().groupId()).isEqualTo(group.getId());
    assertThat(response.getBody().semestreId()).isEqualTo(semester.getId());
    assertThat(response.getBody().dateDebut()).isEqualTo(LocalDate.of(2025, 9, 1));
    assertThat(response.getBody().dateFin()).isNull();
  }

  @Test
  void admin_can_assign_student_to_group_with_date_fin() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 31));

    ResponseEntity<StudentGroupAssignment> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            StudentGroupAssignment.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().dateFin()).isEqualTo(LocalDate.of(2026, 1, 31));
  }

  @Test
  void unauthenticated_cannot_assign_student_to_group() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/users/" + student.getId() + "/group-assignments", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void student_cannot_assign_student_to_group() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_assign_student_to_group() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void assign_to_nonexistent_student_returns_not_found() {
    UUID unknownId = UUID.randomUUID();
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + unknownId + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_with_nonexistent_group_returns_not_found() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            UUID.randomUUID(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_with_nonexistent_semester_returns_not_found() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), UUID.randomUUID(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_duplicate_for_same_semester_returns_conflict() {
    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    restTemplate.exchange(
        "/users/" + student.getId() + "/group-assignments",
        HttpMethod.POST,
        authenticated(request, adminToken),
        StudentGroupAssignment.class);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void assign_to_non_student_user_returns_bad_request() {
    UserEntity adminUser = userRepository.findByEmail("admin+" + adminToken).orElse(null);
    if (adminUser == null) {
      adminUser = registerUser("adminlookup+" + UUID.randomUUID() + "@school.mg", UserRole.ADMIN);
    }

    StudentGroupAssignmentCreateRequest request =
        new StudentGroupAssignmentCreateRequest(
            group.getId(), semester.getId(), LocalDate.of(2025, 9, 1), null);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + adminUser.getId() + "/group-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void assign_with_null_group_id_returns_bad_request() {
    String requestJson =
        "{\"semestreId\":\"" + semester.getId() + "\",\"dateDebut\":\"2025-09-01\"}";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.set("Content-Type", "application/json");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            new HttpEntity<>(requestJson, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void assign_with_null_semestre_id_returns_bad_request() {
    String requestJson = "{\"groupId\":\"" + group.getId() + "\",\"dateDebut\":\"2025-09-01\"}";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.set("Content-Type", "application/json");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            new HttpEntity<>(requestJson, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void assign_with_null_date_debut_returns_bad_request() {
    String requestJson =
        "{\"groupId\":\"" + group.getId() + "\",\"semestreId\":\"" + semester.getId() + "\"}";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.set("Content-Type", "application/json");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.POST,
            new HttpEntity<>(requestJson, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void history_returns_assignments_ordered_by_date_debut() {
    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(student)
            .group(group)
            .semestre(semester)
            .dateDebut(LocalDate.of(2026, 1, 1))
            .build());

    PromotionEntity promotion2 =
        promotionRepository.save(
            PromotionEntity.builder()
                .label("Promotion " + UUID.randomUUID())
                .entryYear(Math.abs(UUID.randomUUID().hashCode()))
                .build());

    SemesterEntity semester2 =
        semesterRepository.save(
            SemesterEntity.builder().promotion(promotion2).number(2).cursusYear(1).build());

    AcademicGroupEntity group2 =
        academicGroupRepository.save(
            AcademicGroupEntity.builder()
                .reference("K2-" + UUID.randomUUID().toString().substring(0, 8))
                .parcours(Parcours.EL)
                .semester(semester2)
                .build());

    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(student)
            .group(group2)
            .semestre(semester2)
            .dateDebut(LocalDate.of(2025, 9, 1))
            .build());

    ResponseEntity<StudentGroupAssignment[]> response =
        restTemplate.exchange(
            "/users/" + student.getId() + "/group-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            StudentGroupAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody()[0].dateDebut()).isEqualTo(LocalDate.of(2025, 9, 1));
    assertThat(response.getBody()[1].dateDebut()).isEqualTo(LocalDate.of(2026, 1, 1));
  }
}
