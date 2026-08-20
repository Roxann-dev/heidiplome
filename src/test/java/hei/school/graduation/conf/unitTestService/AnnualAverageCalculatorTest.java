package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.GroupIsolationService;
import hei.school.graduation.service.calculator.AnnualAverageCalculator;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
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
class AnnualAverageCalculatorTest {

  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private SemesterRepository semestreRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private GroupIsolationService groupIsolationService;
  @Mock private CourseAverageCalculator courseAverageCalculator;

  private AnnualAverageCalculator calculator;

  private UUID studentId;
  private UUID promotionId;
  private int anneeCursus;

  @BeforeEach
  void setUp() {
    calculator =
        new AnnualAverageCalculator(
            studentGroupAssignmentRepository,
            semestreRepository,
            courseRepository,
            groupIsolationService,
            courseAverageCalculator);
    studentId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    anneeCursus = 1;
  }

  private SemesterEntity semestre(UUID id) {
    PromotionEntity promotion = PromotionEntity.builder().id(promotionId).build();
    return SemesterEntity.builder().id(id).promotion(promotion).cursusYear(anneeCursus).build();
  }

  private void mockPromotionResolution() {
    SemesterEntity anySemestre = semestre(UUID.randomUUID());
    StudentGroupAssignmentEntity anyAssignment =
        StudentGroupAssignmentEntity.builder()
            .semestre(anySemestre)
            .dateDebut(LocalDate.now())
            .build();

    when(studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(studentId))
        .thenReturn(List.of(anyAssignment));
  }

  @Test
  void compute_shouldReturnWeightedAverage_whenAllCoursesAreComplete() {
    mockPromotionResolution();

    UUID s1Id = UUID.randomUUID();
    UUID s2Id = UUID.randomUUID();
    when(semestreRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus))
        .thenReturn(List.of(semestre(s1Id), semestre(s2Id)));

    UUID course1 = UUID.randomUUID();
    UUID course2 = UUID.randomUUID();

    when(groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, s1Id))
        .thenReturn(List.of(course1));
    when(groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, s2Id))
        .thenReturn(List.of(course2));

    when(courseAverageCalculator.compute(course1, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("14.00"), StatutReleve.COMPLET));
    when(courseAverageCalculator.compute(course2, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("10.00"), StatutReleve.COMPLET));

    when(courseRepository.findById(course1))
        .thenReturn(Optional.of(CourseEntity.builder().id(course1).credits(5).build()));
    when(courseRepository.findById(course2))
        .thenReturn(Optional.of(CourseEntity.builder().id(course2).credits(3).build()));

    var result = calculator.compute(studentId, anneeCursus);

    assertThat(result.moyenneGenerale()).isEqualByComparingTo("12.50");
    assertThat(result.totalCredits()).isEqualTo(8);
    assertThat(result.complet()).isTrue();
  }

  @Test
  void compute_shouldExcludeProvisoireCourseAndMarkIncomplete() {
    mockPromotionResolution();

    UUID s1Id = UUID.randomUUID();
    when(semestreRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus))
        .thenReturn(List.of(semestre(s1Id)));

    UUID courseComplet = UUID.randomUUID();
    UUID courseProvisoire = UUID.randomUUID();

    when(groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, s1Id))
        .thenReturn(List.of(courseComplet, courseProvisoire));

    when(courseAverageCalculator.compute(courseComplet, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("16.00"), StatutReleve.COMPLET));
    when(courseAverageCalculator.compute(courseProvisoire, studentId))
        .thenReturn(new CourseAverageCalculator.CourseAverageResult(null, StatutReleve.PROVISOIRE));

    when(courseRepository.findById(courseComplet))
        .thenReturn(Optional.of(CourseEntity.builder().id(courseComplet).credits(5).build()));

    var result = calculator.compute(studentId, anneeCursus);

    assertThat(result.moyenneGenerale()).isEqualByComparingTo("16.00");
    assertThat(result.totalCredits()).isEqualTo(5);
    assertThat(result.complet()).isFalse();
  }

  @Test
  void compute_shouldReturnZero_whenStudentFollowsNoCourse() {
    mockPromotionResolution();

    UUID s1Id = UUID.randomUUID();
    when(semestreRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus))
        .thenReturn(List.of(semestre(s1Id)));
    when(groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, s1Id))
        .thenReturn(List.of());

    var result = calculator.compute(studentId, anneeCursus);

    assertThat(result.moyenneGenerale()).isEqualByComparingTo("0.00");
    assertThat(result.totalCredits()).isZero();
    assertThat(result.complet()).isTrue();
  }

  @Test
  void compute_shouldAggregateMultipleCoursesInSameSemestre() {
    mockPromotionResolution();

    UUID s1Id = UUID.randomUUID();
    when(semestreRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus))
        .thenReturn(List.of(semestre(s1Id)));

    UUID courseA = UUID.randomUUID();
    UUID courseB = UUID.randomUUID();
    UUID courseC = UUID.randomUUID();

    when(groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, s1Id))
        .thenReturn(List.of(courseA, courseB, courseC));

    when(courseAverageCalculator.compute(courseA, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("20.00"), StatutReleve.COMPLET));
    when(courseAverageCalculator.compute(courseB, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("0.00"), StatutReleve.COMPLET));
    when(courseAverageCalculator.compute(courseC, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("10.00"), StatutReleve.COMPLET));

    when(courseRepository.findById(courseA))
        .thenReturn(Optional.of(CourseEntity.builder().id(courseA).credits(2).build()));
    when(courseRepository.findById(courseB))
        .thenReturn(Optional.of(CourseEntity.builder().id(courseB).credits(2).build()));
    when(courseRepository.findById(courseC))
        .thenReturn(Optional.of(CourseEntity.builder().id(courseC).credits(6).build()));

    var result = calculator.compute(studentId, anneeCursus);

    assertThat(result.moyenneGenerale()).isEqualByComparingTo("10.00");
    assertThat(result.totalCredits()).isEqualTo(10);
    assertThat(result.complet()).isTrue();
  }

  @Test
  void compute_shouldThrowNotFound_whenStudentHasNoGroupAssignmentAtAll() {
    when(studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(studentId))
        .thenReturn(List.of());

    assertThatThrownBy(() -> calculator.compute(studentId, anneeCursus))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(studentId.toString());
  }
}
