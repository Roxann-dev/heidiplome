package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.dto.TeacherCourseAssignmentCreateRequest;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.TeacherCourseAssignment;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
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

class TeacherCourseAssignmentControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String studentToken;
  private String teacherToken;

  private UserEntity teacher;
  private SemesterEntity semester;
  private CourseEntity course;

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
    teacher = registerUser(teacherEmail, UserRole.TEACHER);
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

    course =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("PROG-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Programmation")
                .credits(5)
                .semester(semester)
                .build());
  }

  @AfterEach
  void tearDown() {
    teacherCourseAssignmentRepository.deleteAll();
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

  private <T> HttpEntity<T> authenticated(T body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  @Test
  void admin_can_list_all_assignments() {
    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(teacher)
            .course(course)
            .anneeAcademique(2025)
            .build());

    ResponseEntity<TeacherCourseAssignment[]> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            TeacherCourseAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].teacherId()).isEqualTo(teacher.getId());
    assertThat(response.getBody()[0].courseId()).isEqualTo(course.getId());
    assertThat(response.getBody()[0].anneeAcademique()).isEqualTo(2025);
  }

  @Test
  void admin_can_filter_assignments_by_teacher_id() {
    UserEntity otherTeacher =
        registerUser("teacher2+" + UUID.randomUUID() + "@school.mg", UserRole.TEACHER);

    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(teacher)
            .course(course)
            .anneeAcademique(2025)
            .build());

    CourseEntity otherCourse =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("MATH-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Mathématiques")
                .credits(4)
                .semester(semester)
                .build());

    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(otherTeacher)
            .course(otherCourse)
            .anneeAcademique(2025)
            .build());

    ResponseEntity<TeacherCourseAssignment[]> response =
        restTemplate.exchange(
            "/teacher-course-assignments?teacherId=" + teacher.getId(),
            HttpMethod.GET,
            authenticatedGet(adminToken),
            TeacherCourseAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].teacherId()).isEqualTo(teacher.getId());
  }

  @Test
  void admin_can_filter_assignments_by_annee_academique() {
    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(teacher)
            .course(course)
            .anneeAcademique(2025)
            .build());

    CourseEntity otherCourse =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("MATH-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Mathématiques")
                .credits(4)
                .semester(semester)
                .build());

    UserEntity otherTeacher =
        registerUser("teacher2+" + UUID.randomUUID() + "@school.mg", UserRole.TEACHER);

    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(otherTeacher)
            .course(otherCourse)
            .anneeAcademique(2024)
            .build());

    ResponseEntity<TeacherCourseAssignment[]> response =
        restTemplate.exchange(
            "/teacher-course-assignments?anneeAcademique=2025",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            TeacherCourseAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].anneeAcademique()).isEqualTo(2025);
  }

  @Test
  void admin_can_filter_assignments_by_teacher_and_year() {
    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(teacher)
            .course(course)
            .anneeAcademique(2025)
            .build());

    CourseEntity otherCourse =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("MATH-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Mathématiques")
                .credits(4)
                .semester(semester)
                .build());

    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(teacher)
            .course(otherCourse)
            .anneeAcademique(2024)
            .build());

    ResponseEntity<TeacherCourseAssignment[]> response =
        restTemplate.exchange(
            "/teacher-course-assignments?teacherId=" + teacher.getId() + "&anneeAcademique=2025",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            TeacherCourseAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].anneeAcademique()).isEqualTo(2025);
  }

  @Test
  void unauthenticated_cannot_list_assignments() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void student_cannot_list_assignments() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_list_assignments() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void list_returns_empty_when_no_assignments() {
    ResponseEntity<TeacherCourseAssignment[]> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            TeacherCourseAssignment[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void admin_can_create_assignment() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2025);

    ResponseEntity<TeacherCourseAssignment> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            TeacherCourseAssignment.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().teacherId()).isEqualTo(teacher.getId());
    assertThat(response.getBody().courseId()).isEqualTo(course.getId());
    assertThat(response.getBody().anneeAcademique()).isEqualTo(2025);
  }

  @Test
  void unauthenticated_cannot_create_assignment() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2025);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/teacher-course-assignments", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void student_cannot_create_assignment() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2025);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_create_assignment() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2025);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void create_assignment_with_nonexistent_teacher_returns_not_found() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(UUID.randomUUID(), course.getId(), 2025);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_assignment_with_nonexistent_course_returns_not_found() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), UUID.randomUUID(), 2025);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_assignment_with_non_teacher_user_returns_bad_request() {
    UserEntity student = userRepository.findByEmail("student+" + studentToken).orElse(null);
    if (student == null) {
      student = registerUser("studentlookup+" + UUID.randomUUID() + "@school.mg", UserRole.STUDENT);
    }

    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(student.getId(), course.getId(), 2025);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_duplicate_assignment_returns_conflict() {
    TeacherCourseAssignmentCreateRequest request =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2025);

    restTemplate.exchange(
        "/teacher-course-assignments",
        HttpMethod.POST,
        authenticated(request, adminToken),
        TeacherCourseAssignment.class);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void create_assignment_with_null_teacher_id_returns_bad_request() {
    String requestJson = "{\"courseId\":\"" + course.getId() + "\",\"anneeAcademique\":2025}";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.set("Content-Type", "application/json");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            new HttpEntity<>(requestJson, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_assignment_with_null_course_id_returns_bad_request() {
    String requestJson = "{\"teacherId\":\"" + teacher.getId() + "\",\"anneeAcademique\":2025}";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.set("Content-Type", "application/json");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            new HttpEntity<>(requestJson, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_assignment_with_null_annee_academique_returns_bad_request() {
    String requestJson =
        "{\"teacherId\":\"" + teacher.getId() + "\",\"courseId\":\"" + course.getId() + "\"}";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.set("Content-Type", "application/json");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            new HttpEntity<>(requestJson, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_assignment_different_year_succeeds() {
    TeacherCourseAssignmentCreateRequest request2025 =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2025);

    restTemplate.exchange(
        "/teacher-course-assignments",
        HttpMethod.POST,
        authenticated(request2025, adminToken),
        TeacherCourseAssignment.class);

    TeacherCourseAssignmentCreateRequest request2024 =
        new TeacherCourseAssignmentCreateRequest(teacher.getId(), course.getId(), 2024);

    ResponseEntity<TeacherCourseAssignment> response =
        restTemplate.exchange(
            "/teacher-course-assignments",
            HttpMethod.POST,
            authenticated(request2024, adminToken),
            TeacherCourseAssignment.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().anneeAcademique()).isEqualTo(2024);
  }
}
