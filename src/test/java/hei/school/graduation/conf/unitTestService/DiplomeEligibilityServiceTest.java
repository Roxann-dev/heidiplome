package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.DiplomeEligibilityService;
import hei.school.graduation.service.GroupIsolationService;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiplomeEligibilityServiceTest {

  @Mock private SemesterRepository semesterRepository;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private GroupIsolationService groupIsolationService;
  @Mock private CourseAverageCalculator courseAverageCalculator;
  @Mock private CourseRepository courseRepository;

  private DiplomeEligibilityService service;

  private UUID studentId;
  private UUID promotionId;
  private UUID semestreId;

  @BeforeEach
  void setUp() {
    service =
        new DiplomeEligibilityService(
            semesterRepository,
            studentGroupAssignmentRepository,
            groupIsolationService,
            courseAverageCalculator,
            courseRepository);
    studentId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    semestreId = UUID.randomUUID();
  }

  @Test
  void evaluate_shouldReturnDiplome_whenAllCoursesHaveAverageAtLeastTen() {
    SemesterEntity semestre = SemesterEntity.builder().id(semestreId).build();
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(semestre));
    when(studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(true);

    UUID course1 = UUID.randomUUID();
    UUID course2 = UUID.randomUUID();
    when(groupIsolationService.resolveFollowedCourseIds(studentId, semestreId))
        .thenReturn(List.of(course1, course2));

    when(courseAverageCalculator.compute(course1, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("12.00"), StatutReleve.COMPLET));
    when(courseAverageCalculator.compute(course2, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("15.00"), StatutReleve.COMPLET));

    when(courseRepository.findById(course1))
        .thenReturn(Optional.of(CourseEntity.builder().id(course1).credits(5).build()));
    when(courseRepository.findById(course2))
        .thenReturn(Optional.of(CourseEntity.builder().id(course2).credits(5).build()));

    var result = service.evaluate(studentId, promotionId);

    assertThat(result.diplome()).isTrue();
    assertThat(result.moyenneCumulee()).isEqualByComparingTo("13.50");
  }

  @Test
  void evaluate_shouldReturnNotDiplome_whenOneCourseIsBelowTen() {
    SemesterEntity semestre = SemesterEntity.builder().id(semestreId).build();
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(semestre));
    when(studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(true);

    UUID course1 = UUID.randomUUID();
    when(groupIsolationService.resolveFollowedCourseIds(studentId, semestreId))
        .thenReturn(List.of(course1));

    when(courseAverageCalculator.compute(course1, studentId))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("9.50"), StatutReleve.COMPLET));

    when(courseRepository.findById(course1))
        .thenReturn(Optional.of(CourseEntity.builder().id(course1).credits(5).build()));

    var result = service.evaluate(studentId, promotionId);

    assertThat(result.diplome()).isFalse();
  }

  @Test
  void evaluate_shouldReturnNotDiplome_whenOneCourseIsProvisoire() {
    SemesterEntity semestre = SemesterEntity.builder().id(semestreId).build();
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(semestre));
    when(studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(true);

    UUID course1 = UUID.randomUUID();
    when(groupIsolationService.resolveFollowedCourseIds(studentId, semestreId))
        .thenReturn(List.of(course1));

    when(courseAverageCalculator.compute(course1, studentId))
        .thenReturn(new CourseAverageCalculator.CourseAverageResult(null, StatutReleve.PROVISOIRE));

    var result = service.evaluate(studentId, promotionId);

    assertThat(result.diplome()).isFalse();
  }

  @Test
  void evaluate_shouldSkipSemestreNotYetReached() {
    SemesterEntity semestre = SemesterEntity.builder().id(semestreId).build();
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(semestre));
    when(studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(false);

    var result = service.evaluate(studentId, promotionId);

    assertThat(result.diplome()).isFalse();
    assertThat(result.moyenneCumulee()).isEqualByComparingTo("0");
  }

  @Test
  void evaluate_shouldReturnNotDiplome_whenStudentFollowsNoCourseAtAll() {
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId)).thenReturn(List.of());

    var result = service.evaluate(studentId, promotionId);

    assertThat(result.diplome()).isFalse();
    assertThat(result.moyenneCumulee()).isEqualByComparingTo("0");
  }
}
