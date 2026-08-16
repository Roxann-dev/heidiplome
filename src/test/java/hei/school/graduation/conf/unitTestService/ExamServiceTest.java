package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.security.UserPrincipal;
import hei.school.graduation.service.ExamService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  @Mock private CourseRepository courseRepository;
  @Mock private ExamRepository examRepository;
  @Mock private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;

  private ExamService service;

  private UUID courseId;
  private UUID teacherId;
  private CourseEntity course;
  private LocalDate examDate;
  private UserEntity adminUser;
  private UserEntity teacherUser;

  @BeforeEach
  void setUp() {
    service = new ExamService(courseRepository, examRepository, teacherCourseAssignmentRepository);
    courseId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
    course = CourseEntity.builder().id(courseId).build();
    examDate = LocalDate.now();
    adminUser = UserEntity.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).build();
    teacherUser = UserEntity.builder().id(teacherId).role(UserRole.TEACHER).build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(UserEntity user) {
    UserPrincipal principal = new UserPrincipal(user);
    Authentication auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  void create_shouldSaveExam_whenNormalCoefficientSumStaysUnderOrEqualToOne() {
    authenticateAs(adminUser);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of());
    when(examRepository.save(any(ExamEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ExamEntity result = service.create(courseId, examDate, new BigDecimal("0.50"), ExamType.NORMAL);

    assertThat(result.getCoefficient()).isEqualByComparingTo("0.50");
    assertThat(result.getType()).isEqualTo(ExamType.NORMAL);
    assertThat(result.getCourse()).isEqualTo(course);
    verify(examRepository).save(any(ExamEntity.class));
  }

  @Test
  void create_shouldThrowNotFound_whenCourseDoesNotExist() {
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.create(courseId, examDate, new BigDecimal("0.50"), ExamType.NORMAL))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(courseId.toString());

    verifyNoInteractions(examRepository, teacherCourseAssignmentRepository);
  }

  @Test
  void create_shouldThrowConflict_whenNormalSumWouldExceedOne() {
    authenticateAs(adminUser);
    ExamEntity existing =
        ExamEntity.builder()
            .id(UUID.randomUUID())
            .course(course)
            .examDate(examDate)
            .coefficient(new BigDecimal("0.70"))
            .type(ExamType.NORMAL)
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(existing));

    assertThatThrownBy(
            () -> service.create(courseId, examDate, new BigDecimal("0.40"), ExamType.NORMAL))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("would exceed 1");

    verify(examRepository, never()).save(any());
  }

  @Test
  void create_shouldSaveExam_whenNormalSumExactlyEqualsOne() {
    authenticateAs(adminUser);
    ExamEntity existing =
        ExamEntity.builder()
            .id(UUID.randomUUID())
            .course(course)
            .examDate(examDate)
            .coefficient(new BigDecimal("0.67"))
            .type(ExamType.NORMAL)
            .build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(existing));
    when(examRepository.save(any(ExamEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ExamEntity result = service.create(courseId, examDate, new BigDecimal("0.33"), ExamType.NORMAL);

    assertThat(result.getCoefficient()).isEqualByComparingTo("0.33");
    verify(examRepository).save(any(ExamEntity.class));
  }

  @Test
  void create_shouldIgnoreExistingNormalSum_whenTypeIsRattrapage() {
    authenticateAs(adminUser);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.save(any(ExamEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ExamEntity result = service.create(courseId, examDate, BigDecimal.ONE, ExamType.RATTRAPAGE);

    assertThat(result.getType()).isEqualTo(ExamType.RATTRAPAGE);
    assertThat(result.getCoefficient()).isEqualByComparingTo("1");
    verify(examRepository, never()).findByCourseId(any());
    verify(examRepository).save(any(ExamEntity.class));
  }

  @Test
  void create_shouldDefaultToNormal_whenTypeIsNull() {
    authenticateAs(adminUser);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of());
    when(examRepository.save(any(ExamEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ExamEntity result = service.create(courseId, examDate, new BigDecimal("0.50"), null);

    assertThat(result.getType()).isEqualTo(ExamType.NORMAL);
  }

  @Test
  void create_shouldSaveExam_whenTeacherIsAssignedToCourse() {
    authenticateAs(teacherUser);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of());
    when(examRepository.save(any(ExamEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ExamEntity result = service.create(courseId, examDate, new BigDecimal("0.50"), ExamType.NORMAL);

    assertThat(result).isNotNull();
    verify(examRepository).save(any(ExamEntity.class));
  }

  @Test
  void create_shouldThrowAccessDenied_whenTeacherIsNotAssignedToCourse() {
    authenticateAs(teacherUser);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(false);

    assertThatThrownBy(
            () -> service.create(courseId, examDate, new BigDecimal("0.50"), ExamType.NORMAL))
        .isInstanceOf(AccessDeniedException.class);

    verify(examRepository, never()).save(any());
  }

  @Test
  void findByCourse_shouldReturnExams_whenCourseExists() {
    ExamEntity exam1 = ExamEntity.builder().id(UUID.randomUUID()).course(course).build();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1));

    List<ExamEntity> result = service.findByCourse(courseId);

    assertThat(result).containsExactly(exam1);
  }

  @Test
  void findByCourse_shouldThrowNotFound_whenCourseDoesNotExist() {
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(() -> service.findByCourse(courseId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(courseId.toString());

    verifyNoInteractions(examRepository);
  }

  @Test
  void findById_shouldReturnExam_whenExists() {
    UUID examId = UUID.randomUUID();
    ExamEntity exam = ExamEntity.builder().id(examId).course(course).build();
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

    ExamEntity result = service.findById(examId);

    assertThat(result).isEqualTo(exam);
  }

  @Test
  void findById_shouldThrowNotFound_whenExamDoesNotExist() {
    UUID examId = UUID.randomUUID();
    when(examRepository.findById(examId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(examId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(examId.toString());
  }
}
