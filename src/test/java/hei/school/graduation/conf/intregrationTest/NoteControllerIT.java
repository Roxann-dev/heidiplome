package hei.school.graduation.conf.intregrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.conf.FacadeIT;
import hei.school.graduation.dto.LoginRequest;
import hei.school.graduation.dto.LoginResponse;
import hei.school.graduation.dto.NoteCreateRequest;
import hei.school.graduation.dto.NoteUpdateRequest;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.Note;
import hei.school.graduation.model.NoteHistory;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteHistoryRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
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

class NoteControllerIT extends FacadeIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private SemesterRepository semesterRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private NoteRepository noteRepository;
    @Autowired private NoteHistoryRepository noteHistoryRepository;
    @Autowired private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
    @Autowired private AcademicGroupRepository academicGroupRepository;
    @Autowired private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
    @Autowired private CourseGroupAssignmentRepository courseGroupAssignmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private ExamEntity exam;
    private UUID studentId;
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

        CourseEntity course =
                courseRepository.save(
                        CourseEntity.builder()
                                .referenceCs("PROG1")
                                .title("Programmation 1")
                                .credits(5)
                                .semester(semester)
                                .build());

        exam =
                examRepository.save(
                        ExamEntity.builder()
                                .course(course)
                                .examDate(LocalDate.of(2026, 1, 15))
                                .coefficient(new BigDecimal("1.00"))
                                .type(ExamType.NORMAL)
                                .build());

        AcademicGroupEntity group =
                academicGroupRepository.save(
                        AcademicGroupEntity.builder()
                                .reference("S1-A")
                                .semester(semester)
                                .build());

        courseGroupAssignmentRepository.save(
                CourseGroupAssignmentEntity.builder()
                        .course(course)
                        .group(group)
                        .semestre(semester)
                        .build());

        adminToken = registerAndLogin("admin+" + UUID.randomUUID() + "@school.mg", UserRole.ADMIN);

        String teacherEmail = "teacher+" + UUID.randomUUID() + "@school.mg";
        UUID teacherId = createUser(teacherEmail, UserRole.TEACHER);
        teacherToken = login(teacherEmail);

        teacherCourseAssignmentRepository.save(
                TeacherCourseAssignmentEntity.builder()
                        .teacher(userRepository.findById(teacherId).orElseThrow())
                        .course(course)
                        .anneeAcademique(2023)
                        .build());

        String studentEmail = "student+" + UUID.randomUUID() + "@school.mg";
        studentId = createUser(studentEmail, UserRole.STUDENT);
        studentToken = login(studentEmail);

        studentGroupAssignmentRepository.save(
                StudentGroupAssignmentEntity.builder()
                        .student(userRepository.findById(studentId).orElseThrow())
                        .group(group)
                        .semestre(semester)
                        .dateDebut(LocalDate.of(2023, 9, 1))
                        .build());
    }

    @AfterEach
    void tearDown() {
        noteHistoryRepository.deleteAll();
        noteRepository.deleteAll();
        studentGroupAssignmentRepository.deleteAll();
        courseGroupAssignmentRepository.deleteAll();
        teacherCourseAssignmentRepository.deleteAll();
        academicGroupRepository.deleteAll();
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

    private <T> HttpEntity<T> authenticated(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void teacher_can_create_note() {
        NoteCreateRequest request = new NoteCreateRequest(studentId, new BigDecimal("14.50"));

        ResponseEntity<Note> response =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(request, teacherToken),
                        Note.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().studentId()).isEqualTo(studentId);
        assertThat(response.getBody().value()).isEqualByComparingTo("14.50");
    }

    @Test
    void admin_can_create_note() {
        NoteCreateRequest request = new NoteCreateRequest(studentId, new BigDecimal("16.00"));

        ResponseEntity<Note> response =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(request, adminToken),
                        Note.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void student_cannot_create_note() {
        NoteCreateRequest request = new NoteCreateRequest(studentId, new BigDecimal("14.50"));

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(request, studentToken),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void create_note_with_unknown_exam_returns_404() {
        NoteCreateRequest request = new NoteCreateRequest(studentId, new BigDecimal("14.50"));

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/examens/" + UUID.randomUUID() + "/notes",
                        HttpMethod.POST,
                        authenticated(request, teacherToken),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void teacher_can_list_notes_by_exam() {
        NoteCreateRequest request = new NoteCreateRequest(studentId, new BigDecimal("14.50"));
        restTemplate.exchange(
                "/examens/" + exam.getId() + "/notes",
                HttpMethod.POST,
                authenticated(request, teacherToken),
                Note.class);

        ResponseEntity<Note[]> response =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.GET,
                        authenticatedGet(teacherToken),
                        Note[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void student_cannot_list_notes_by_exam() {
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.GET,
                        authenticatedGet(studentToken),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void teacher_can_update_note() {
        NoteCreateRequest createRequest = new NoteCreateRequest(studentId, new BigDecimal("14.50"));
        ResponseEntity<Note> createResponse =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(createRequest, teacherToken),
                        Note.class);
        UUID noteId = createResponse.getBody().id();

        NoteUpdateRequest updateRequest =
                new NoteUpdateRequest(new BigDecimal("17.00"), "Erreur de saisie corrigée");

        ResponseEntity<Note> response =
                restTemplate.exchange(
                        "/notes/" + noteId,
                        HttpMethod.PATCH,
                        authenticated(updateRequest, teacherToken),
                        Note.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().value()).isEqualByComparingTo("17.00");
    }

    @Test
    void student_cannot_update_note() {
        NoteCreateRequest createRequest = new NoteCreateRequest(studentId, new BigDecimal("14.50"));
        ResponseEntity<Note> createResponse =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(createRequest, teacherToken),
                        Note.class);
        UUID noteId = createResponse.getBody().id();

        NoteUpdateRequest updateRequest = new NoteUpdateRequest(new BigDecimal("20.00"), "Triche");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/notes/" + noteId,
                        HttpMethod.PATCH,
                        authenticated(updateRequest, studentToken),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void update_unknown_note_returns_404() {
        NoteUpdateRequest updateRequest = new NoteUpdateRequest(new BigDecimal("17.00"), "Correction");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/notes/" + UUID.randomUUID(),
                        HttpMethod.PATCH,
                        authenticated(updateRequest, teacherToken),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void teacher_can_view_note_history_after_update() {
        NoteCreateRequest createRequest = new NoteCreateRequest(studentId, new BigDecimal("14.50"));
        ResponseEntity<Note> createResponse =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(createRequest, teacherToken),
                        Note.class);
        UUID noteId = createResponse.getBody().id();

        NoteUpdateRequest updateRequest =
                new NoteUpdateRequest(new BigDecimal("17.00"), "Erreur de saisie corrigée");
        restTemplate.exchange(
                "/notes/" + noteId,
                HttpMethod.PATCH,
                authenticated(updateRequest, teacherToken),
                Note.class);

        ResponseEntity<NoteHistory[]> response =
                restTemplate.exchange(
                        "/notes/" + noteId + "/historique",
                        HttpMethod.GET,
                        authenticatedGet(teacherToken),
                        NoteHistory[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].previousValue()).isEqualByComparingTo("14.50");
    }

    @Test
    void student_can_view_note_history() {
        NoteCreateRequest createRequest = new NoteCreateRequest(studentId, new BigDecimal("14.50"));
        ResponseEntity<Note> createResponse =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        authenticated(createRequest, teacherToken),
                        Note.class);
        UUID noteId = createResponse.getBody().id();

        ResponseEntity<NoteHistory[]> response =
                restTemplate.exchange(
                        "/notes/" + noteId + "/historique",
                        HttpMethod.GET,
                        authenticatedGet(studentToken),
                        NoteHistory[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unauthenticated_cannot_create_note() {
        NoteCreateRequest request = new NoteCreateRequest(studentId, new BigDecimal("14.50"));

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/examens/" + exam.getId() + "/notes",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}