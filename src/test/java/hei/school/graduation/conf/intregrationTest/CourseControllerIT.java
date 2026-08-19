package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.CourseCreateRequest;
import hei.school.graduation.dto.CourseGroupAssignmentCreateRequest;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Course;
import hei.school.graduation.model.CourseGroupAssignment;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.CourseRepository;
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

class CourseControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private AcademicGroupRepository academicGroupRepository;
  @Autowired private CourseGroupAssignmentRepository courseGroupAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String adminToken;
  private String studentToken;
  private String teacherToken;

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
    registerUser(studentEmail, UserRole.STUDENT);
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
    courseGroupAssignmentRepository.deleteAll();
    academicGroupRepository.deleteAll();
    courseRepository.deleteAll();
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

  private <T> HttpEntity<T> authenticated(T body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  private CourseEntity saveCourse(String referenceCs, SemesterEntity semestre) {
    return courseRepository.save(
        CourseEntity.builder()
            .referenceCs(referenceCs)
            .title("Titre " + referenceCs)
            .credits(5)
            .semester(semestre)
            .build());
  }

  // GET /courses

  @Test
  void admin_can_list_all_courses() {
    saveCourse("PROG1", semester);
    saveCourse("PROG2", semester);

    ResponseEntity<Course[]> response =
        restTemplate.exchange(
            "/courses", HttpMethod.GET, authenticatedGet(adminToken), Course[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void student_can_list_all_courses() {
    saveCourse("PROG1", semester);

    ResponseEntity<Course[]> response =
        restTemplate.exchange(
            "/courses", HttpMethod.GET, authenticatedGet(studentToken), Course[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void teacher_can_list_all_courses() {
    saveCourse("PROG1", semester);

    ResponseEntity<Course[]> response =
        restTemplate.exchange(
            "/courses", HttpMethod.GET, authenticatedGet(teacherToken), Course[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void unauthenticated_cannot_list_courses() {
    ResponseEntity<String> response =
        restTemplate.exchange("/courses", HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void admin_can_filter_courses_by_semestre() {
    PromotionEntity otherPromotion =
        promotionRepository.save(
            PromotionEntity.builder()
                .label("Promotion " + UUID.randomUUID())
                .entryYear(Math.abs(UUID.randomUUID().hashCode()))
                .build());
    SemesterEntity otherSemester =
        semesterRepository.save(
            SemesterEntity.builder().promotion(otherPromotion).number(2).cursusYear(1).build());

    saveCourse("PROG1", semester);
    saveCourse("PROG2", otherSemester);

    ResponseEntity<Course[]> response =
        restTemplate.exchange(
            "/courses?semestreId=" + semester.getId(),
            HttpMethod.GET,
            authenticatedGet(adminToken),
            Course[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].referenceCs()).isEqualTo("PROG1");
  }

  @Test
  void admin_can_get_course_by_id() {
    CourseEntity course = saveCourse("PROG1", semester);

    ResponseEntity<Course> response =
        restTemplate.exchange(
            "/courses/" + course.getId(),
            HttpMethod.GET,
            authenticatedGet(adminToken),
            Course.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().referenceCs()).isEqualTo("PROG1");
  }

  @Test
  void get_nonexistent_course_returns_not_found() {
    UUID unknownCourseId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + unknownCourseId,
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void unauthenticated_cannot_get_course_by_id() {
    CourseEntity course = saveCourse("PROG1", semester);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void admin_can_create_course() {
    CourseCreateRequest request =
        new CourseCreateRequest("PROG4", "Programmation avancée", 5, semester.getId());

    ResponseEntity<Course> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, adminToken), Course.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().referenceCs()).isEqualTo("PROG4");
    assertThat(response.getBody().credits()).isEqualTo(5);
    assertThat(response.getBody().semesterId()).isEqualTo(semester.getId());
  }

  @Test
  void student_cannot_create_course() {
    CourseCreateRequest request =
        new CourseCreateRequest("PROG4", "Programmation avancée", 5, semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, studentToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_create_course() {
    CourseCreateRequest request =
        new CourseCreateRequest("PROG4", "Programmation avancée", 5, semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, teacherToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_create_course() {
    CourseCreateRequest request =
        new CourseCreateRequest("PROG4", "Programmation avancée", 5, semester.getId());

    ResponseEntity<String> response = restTemplate.postForEntity("/courses", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void create_course_with_blank_reference_returns_bad_request() {
    CourseCreateRequest request = new CourseCreateRequest("", "Titre", 5, semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, adminToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_course_with_zero_credits_returns_bad_request() {
    CourseCreateRequest request = new CourseCreateRequest("PROG4", "Titre", 0, semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, adminToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_course_for_nonexistent_semester_returns_not_found() {
    CourseCreateRequest request = new CourseCreateRequest("PROG4", "Titre", 5, UUID.randomUUID());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, adminToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_course_with_duplicate_reference_in_same_semester_returns_conflict() {
    saveCourse("PROG4", semester);
    CourseCreateRequest request = new CourseCreateRequest("PROG4", "Titre", 5, semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses", HttpMethod.POST, authenticated(request, adminToken), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void admin_can_assign_group_to_course() {
    CourseEntity course = saveCourse("PROG4", semester);
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(group.getId(), semester.getId());

    ResponseEntity<CourseGroupAssignment> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            CourseGroupAssignment.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().courseId()).isEqualTo(course.getId());
    assertThat(response.getBody().groupId()).isEqualTo(group.getId());
  }

  @Test
  void student_cannot_assign_group_to_course() {
    CourseEntity course = saveCourse("PROG4", semester);
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(group.getId(), semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_assign_group_to_course() {
    CourseEntity course = saveCourse("PROG4", semester);
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(group.getId(), semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_assign_group_to_course() {
    CourseEntity course = saveCourse("PROG4", semester);
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(group.getId(), semester.getId());

    ResponseEntity<String> response =
        restTemplate.postForEntity("/courses/" + course.getId() + "/groups", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void assign_group_to_nonexistent_course_returns_not_found() {
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(group.getId(), semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + UUID.randomUUID() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_nonexistent_group_to_course_returns_not_found() {
    CourseEntity course = saveCourse("PROG4", semester);
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(UUID.randomUUID(), semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_group_to_course_already_associated_returns_conflict() {
    CourseEntity course = saveCourse("PROG4", semester);
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder().reference("K1").semester(semester).build());
    courseGroupAssignmentRepository.save(
        CourseGroupAssignmentEntity.builder()
            .course(course)
            .group(group)
            .semestre(semester)
            .build());

    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(group.getId(), semester.getId());

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/courses/" + course.getId() + "/groups",
            HttpMethod.POST,
            authenticated(request, adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }
}
