package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.ExamCreateRequest;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.Exam;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

class ExamControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private CourseEntity course;
  private UUID teacherId;
  private String adminToken;
  private String teacherToken;
  private String studentToken;

  @BeforeEach
  void setUpRestTemplate() {
    restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  @BeforeEach
  void setUp() {
    PromotionEntity promotion =
        promotionRepository.save(
            PromotionEntity.builder().label("Promotion 2023").entryYear(2023).build());

    SemesterEntity semester =
        semesterRepository.save(
            SemesterEntity.builder().promotion(promotion).number(1).cursusYear(1).build());

    course =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("PROG1")
                .title("Programmation 1")
                .credits(5)
                .semester(semester)
                .build());

    adminToken = registerAndLogin("admin+" + UUID.randomUUID() + "@school.mg", UserRole.ADMIN);

    String teacherEmail = "teacher+" + UUID.randomUUID() + "@school.mg";
    teacherId = createUser(teacherEmail, UserRole.TEACHER);
    teacherToken = login(teacherEmail);

    teacherCourseAssignmentRepository.save(
        TeacherCourseAssignmentEntity.builder()
            .teacher(userRepository.findById(teacherId).orElseThrow())
            .course(course)
            .anneeAcademique(2023)
            .build());

    studentToken =
        registerAndLogin("student+" + UUID.randomUUID() + "@school.mg", UserRole.STUDENT);
  }

  @AfterEach
  void tearDown() {
    teacherCourseAssignmentRepository.deleteAll();
    examRepository.deleteAll();
    courseRepository.deleteAll();
    semesterRepository.deleteAll();
    promotionRepository.deleteAll();
    userRepository.deleteAll();
  }

  private UUID createUser(String email, UserRole role) {
    UserEntity user =
        UserEntity.builder()
            .reference("REF-" + UUID.randomUUID())
            .lastName("Test")
            .firstName("User")
            .email(email)
            .passwordHash(passwordEncoder.encode("P@ssw0rd123"))
            .role(role)
            .build();
    return userRepository.save(user).getId();
  }

  private String login(String email) {
    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity(
            "/auth/login", new LoginRequest(email, "P@ssw0rd123"), LoginResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody().accessToken();
  }

  private String registerAndLogin(String email, UserRole role) {
    createUser(email, role);
    return login(email);
  }

  private HttpEntity<Void> authenticatedGet(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private HttpEntity<ExamCreateRequest> authenticated(ExamCreateRequest body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  @Test
  void admin_can_create_exam() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);

    ResponseEntity<Exam> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.POST,
            authenticated(request, adminToken),
            Exam.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().courseId()).isEqualTo(course.getId());
    assertThat(response.getBody().type()).isEqualTo(ExamType.NORMAL);
  }

  @Test
  void teacher_can_create_exam_for_assigned_course() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);

    ResponseEntity<Exam> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.POST,
            authenticated(request, teacherToken),
            Exam.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void teacher_cannot_create_exam_for_unassigned_course() {
    CourseEntity otherCourse =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("MATH1")
                .title("Mathematiques 1")
                .credits(4)
                .semester(course.getSemester())
                .build());

    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + otherCourse.getId() + "/examens",
            HttpMethod.POST,
            authenticated(request, teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void student_cannot_create_exam() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.POST,
            authenticated(request, studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void create_exam_with_unknown_course_returns_404() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + UUID.randomUUID() + "/examens",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void admin_can_list_exams_by_course() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);
    restTemplate.exchange(
        "/courses/" + course.getId() + "/examens",
        HttpMethod.POST,
        authenticated(request, adminToken),
        Exam.class);

    ResponseEntity<Exam[]> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            Exam[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void student_can_list_exams_by_course() {
    ResponseEntity<Exam[]> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            Exam[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void get_exam_by_id_returns_exam() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);
    ResponseEntity<Exam> createResponse =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.POST,
            authenticated(request, adminToken),
            Exam.class);
    UUID examId = createResponse.getBody().id();

    ResponseEntity<Exam> response =
        restTemplate.exchange(
            "/examens/" + examId, HttpMethod.GET, authenticatedGet(studentToken), Exam.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().id()).isEqualTo(examId);
  }

  @Test
  void get_unknown_exam_returns_404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/examens/" + UUID.randomUUID(),
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void unauthenticated_cannot_create_exam() {
    ExamCreateRequest request =
        new ExamCreateRequest(LocalDate.of(2026, 1, 15), new BigDecimal("1.00"), ExamType.NORMAL);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/examens",
            HttpMethod.POST,
            new HttpEntity<>(request),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
