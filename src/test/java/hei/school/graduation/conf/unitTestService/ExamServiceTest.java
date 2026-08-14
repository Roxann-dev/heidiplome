package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.service.ExamService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  @Mock private CourseRepository courseRepository;
  @Mock private ExamRepository examRepository;

  private ExamService service;

  private UUID courseId;
  private CourseEntity course;
  private LocalDate examDate;

  @BeforeEach
  void setUp() {
    service = new ExamService(courseRepository, examRepository);
    courseId = UUID.randomUUID();
    course = CourseEntity.builder().id(courseId).build();
    examDate = LocalDate.now();
  }

  @Test
  void create_shouldSaveExam_whenNormalCoefficientSumStaysUnderOrEqualToOne() {
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

    verifyNoInteractions(examRepository);
  }

  @Test
  void create_shouldThrowBadRequest_whenNormalSumWouldExceedOne() {
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
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("would exceed 1");

    verify(examRepository, never()).save(any());
  }

  @Test
  void create_shouldSaveExam_whenNormalSumExactlyEqualsOne() {
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
    ExamEntity existingNormal =
        ExamEntity.builder()
            .id(UUID.randomUUID())
            .course(course)
            .examDate(examDate)
            .coefficient(BigDecimal.ONE)
            .type(ExamType.NORMAL)
            .build();

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
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of());
    when(examRepository.save(any(ExamEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ExamEntity result = service.create(courseId, examDate, new BigDecimal("0.50"), null);

    assertThat(result.getType()).isEqualTo(ExamType.NORMAL);
  }
}
