package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.ReleveAnnuel;
import hei.school.graduation.model.ReleveSemester;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.time.LocalDate;
import java.util.Map;
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

class ReleveControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private AcademicGroupRepository academicGroupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseGroupAssignmentRepository courseGroupAssignmentRepository;
  @Autowired private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String studentToken;
  private String teacherToken;

  private UserEntity studentEntity;
  private UserEntity otherStudentEntity;
  private SemesterEntity semester;
  private SemesterEntity semesterWithoutAssignment;

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

    String otherStudentEmail = "other-student+" + UUID.randomUUID() + "@school.mg";
    otherStudentEntity = registerUser(otherStudentEmail, UserRole.STUDENT);

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

    semesterWithoutAssignment =
        semesterRepository.save(
            SemesterEntity.builder().promotion(promotion).number(2).cursusYear(1).build());

    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());

    CourseEntity course =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("PROG1")
                .title("Programmation 1")
                .credits(5)
                .semester(semester)
                .build());

    courseGroupAssignmentRepository.save(
        CourseGroupAssignmentEntity.builder()
            .course(course)
            .group(group)
            .semestre(semester)
            .build());

    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(studentEntity)
            .group(group)
            .semestre(semester)
            .dateDebut(LocalDate.now())
            .build());
  }

  @AfterEach
  void tearDown() {
    studentGroupAssignmentRepository.deleteAll();
    courseGroupAssignmentRepository.deleteAll();
    academicGroupRepository.deleteAll();
    courseRepository.deleteAll();
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

  @Test
  void student_can_view_own_releve_semestre() {
    ResponseEntity<ReleveSemester> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/semestres/" + semester.getId(),
            HttpMethod.GET,
            authenticatedGet(studentToken),
            ReleveSemester.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().studentId()).isEqualTo(studentEntity.getId());
    assertThat(response.getBody().semestreId()).isEqualTo(semester.getId());
    assertThat(response.getBody().lines()).hasSize(1);
  }

  @Test
  void admin_can_view_any_student_releve_semestre() {
    ResponseEntity<ReleveSemester> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/semestres/" + semester.getId(),
            HttpMethod.GET,
            authenticatedGet(adminToken),
            ReleveSemester.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void student_cannot_view_other_student_releve_semestre() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + otherStudentEntity.getId() + "/releves/semestres/" + semester.getId(),
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_view_releve_semestre() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/semestres/" + semester.getId(),
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_view_releve_semestre() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/semestres/" + semester.getId(),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void releve_semestre_for_student_without_group_assignment_returns_not_found() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/"
                + studentEntity.getId()
                + "/releves/semestres/"
                + semesterWithoutAssignment.getId(),
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void student_can_view_own_releve_annuel() {
    ResponseEntity<ReleveAnnuel> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/years/1",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            ReleveAnnuel.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().studentId()).isEqualTo(studentEntity.getId());
    assertThat(response.getBody().cursusYear()).isEqualTo(1);
  }

  @Test
  void admin_can_view_any_student_releve_annuel() {
    ResponseEntity<ReleveAnnuel> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/years/1",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            ReleveAnnuel.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void student_cannot_view_other_student_releve_annuel() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + otherStudentEntity.getId() + "/releves/years/1",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_view_releve_annuel() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/years/1",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_view_releve_annuel() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + studentEntity.getId() + "/releves/years/1",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void releve_annuel_for_student_without_any_group_assignment_returns_not_found() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/students/" + otherStudentEntity.getId() + "/releves/years/1",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void student_can_trigger_own_releve_pdf_generation() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/students/" + studentEntity.getId() + "/releve-pdf",
            authenticatedGet(studentToken),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody()).containsKey("message");
  }

  @Test
  void admin_can_trigger_releve_pdf_generation_for_any_student() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/students/" + studentEntity.getId() + "/releve-pdf",
            authenticatedGet(adminToken),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }

  @Test
  void teacher_cannot_trigger_releve_pdf_generation() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/students/" + studentEntity.getId() + "/releve-pdf",
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_trigger_releve_pdf_generation() {
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/students/" + studentEntity.getId() + "/releve-pdf", HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
