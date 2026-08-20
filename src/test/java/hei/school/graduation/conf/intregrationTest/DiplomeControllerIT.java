package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.file.bucket.BucketComponent;
import hei.school.graduation.model.DiplomeEntry;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

class DiplomeControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private AcademicGroupRepository academicGroupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseGroupAssignmentRepository courseGroupAssignmentRepository;
  @Autowired private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private NoteRepository noteRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockBean private BucketComponent bucketComponent;

  private String adminToken;
  private String studentToken;
  private String teacherToken;

  private PromotionEntity promotion;

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

    promotion =
        promotionRepository.save(
            PromotionEntity.builder()
                .label("Promotion " + UUID.randomUUID())
                .entryYear(Math.abs(UUID.randomUUID().hashCode()))
                .build());
  }

  @AfterEach
  void tearDown() {
    noteRepository.deleteAll();
    examRepository.deleteAll();
    studentGroupAssignmentRepository.deleteAll();
    courseGroupAssignmentRepository.deleteAll();
    academicGroupRepository.deleteAll();
    courseRepository.deleteAll();
    semesterRepository.deleteAll();
    promotionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void admin_can_preview_diplomes_el() {
    UserEntity student =
        createEligibleStudentForPromotion("EL", new BigDecimal("15.00"), new BigDecimal("12.50"));

    ResponseEntity<DiplomeEntry[]> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            DiplomeEntry[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].rang()).isEqualTo(1);
    assertThat(response.getBody()[0].std()).isEqualTo(student.getReference());
    assertThat(response.getBody()[0].nom()).isEqualTo("Test");
    assertThat(response.getBody()[0].prenom()).isEqualTo("User");
    assertThat(response.getBody()[0].moyenneGenerale()).isEqualByComparingTo("13.75");
  }

  @Test
  void admin_can_export_diplomes_el_as_excel() {
    createEligibleStudentForPromotion("EL", new BigDecimal("15.00"), new BigDecimal("12.50"));

    ResponseEntity<byte[]> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes/export?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    assertThat(response.getBody()).isNotEmpty();
  }

  @Test
  void admin_gets_empty_list_when_no_eligible_students() {
    SemesterEntity s6 = saveSemester(6, 3);
    UserEntity student = registerUser("std+" + UUID.randomUUID() + "@school.mg", UserRole.STUDENT);
    AcademicGroupEntity group =
        academicGroupRepository.save(
            AcademicGroupEntity.builder()
                .reference("S6-EL-A")
                .parcours(Parcours.EL)
                .semester(s6)
                .build());
    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(student)
            .group(group)
            .semestre(s6)
            .dateDebut(LocalDate.now())
            .build());

    ResponseEntity<DiplomeEntry[]> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            DiplomeEntry[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void admin_gets_not_found_when_no_s6() {
    saveSemester(1, 1);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void admin_gets_not_found_for_unknown_promotion() {
    UUID unknownPromotionId = UUID.randomUUID();

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + unknownPromotionId + "/diplomes?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(adminToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void student_cannot_preview_diplomes() {
    saveSemester(6, 3);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(studentToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_cannot_preview_diplomes() {
    saveSemester(6, 3);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes?parcours=EL",
            HttpMethod.GET,
            authenticatedGet(teacherToken),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unauthenticated_cannot_preview_diplomes() {
    saveSemester(6, 3);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/promotions/" + promotion.getId() + "/diplomes?parcours=EL",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private UserEntity createEligibleStudentForPromotion(
      String parcoursStr, BigDecimal noteS1, BigDecimal noteS2) {
    UserEntity student = registerUser("std+" + UUID.randomUUID() + "@school.mg", UserRole.STUDENT);
    Parcours parcours = Parcours.valueOf(parcoursStr);

    SemesterEntity s1 = saveSemester(1, 1);
    AcademicGroupEntity groupS1 =
        academicGroupRepository.save(
            AcademicGroupEntity.builder()
                .reference("S1-" + parcoursStr + "-A")
                .parcours(parcours)
                .semester(s1)
                .build());
    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(student)
            .group(groupS1)
            .semestre(s1)
            .dateDebut(LocalDate.of(2025, 9, 1))
            .build());
    CourseEntity courseS1 =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("PROG1-" + parcoursStr)
                .title("Programmation 1")
                .credits(15)
                .semester(s1)
                .build());
    courseGroupAssignmentRepository.save(
        CourseGroupAssignmentEntity.builder().course(courseS1).group(groupS1).semestre(s1).build());
    ExamEntity examS1 =
        examRepository.save(
            ExamEntity.builder()
                .course(courseS1)
                .examDate(LocalDate.of(2026, 1, 20))
                .coefficient(BigDecimal.ONE)
                .type(ExamType.NORMAL)
                .build());
    noteRepository.save(NoteEntity.builder().exam(examS1).student(student).value(noteS1).build());

    SemesterEntity s6 = saveSemester(6, 3);
    AcademicGroupEntity groupS6 =
        academicGroupRepository.save(
            AcademicGroupEntity.builder()
                .reference("S6-" + parcoursStr + "-A")
                .parcours(parcours)
                .semester(s6)
                .build());
    studentGroupAssignmentRepository.save(
        StudentGroupAssignmentEntity.builder()
            .student(student)
            .group(groupS6)
            .semestre(s6)
            .dateDebut(LocalDate.of(2027, 9, 1))
            .build());
    CourseEntity courseS6 =
        courseRepository.save(
            CourseEntity.builder()
                .referenceCs("PROG6-" + parcoursStr)
                .title("Programmation 6")
                .credits(15)
                .semester(s6)
                .build());
    courseGroupAssignmentRepository.save(
        CourseGroupAssignmentEntity.builder().course(courseS6).group(groupS6).semestre(s6).build());
    ExamEntity examS6 =
        examRepository.save(
            ExamEntity.builder()
                .course(courseS6)
                .examDate(LocalDate.of(2028, 1, 20))
                .coefficient(BigDecimal.ONE)
                .type(ExamType.NORMAL)
                .build());
    noteRepository.save(NoteEntity.builder().exam(examS6).student(student).value(noteS2).build());

    return student;
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

  private SemesterEntity saveSemester(int number, int cursusYear) {
    return semesterRepository.save(
        SemesterEntity.builder()
            .promotion(promotion)
            .number(number)
            .cursusYear(cursusYear)
            .build());
  }
}
