package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.GroupIsolationService;
import hei.school.graduation.service.calculator.AnnualAverageCalculator;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import hei.school.graduation.service.pdf.ReleasePdfGenerator;
import java.io.File;
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
class ReleasePdfGeneratorTest {

  @Mock private AnnualAverageCalculator annualAverageCalculator;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private SemesterRepository semesterRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private ExamRepository examRepository;
  @Mock private NoteRepository noteRepository;
  @Mock private GroupIsolationService groupIsolationService;
  @Mock private CourseAverageCalculator courseAverageCalculator;

  private ReleasePdfGenerator generator;

  private UserEntity student;
  private UUID promotionId;
  private UUID semesterId;
  private UUID courseId;
  private UUID examId;

  @BeforeEach
  void setUp() {
    generator =
        new ReleasePdfGenerator(
            annualAverageCalculator,
            studentGroupAssignmentRepository,
            semesterRepository,
            courseRepository,
            examRepository,
            noteRepository,
            groupIsolationService,
            courseAverageCalculator);

    student =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .reference("STD-2023-001")
            .firstName("Roxann")
            .lastName("Rakoto")
            .email("roxann@example.com")
            .role(UserRole.STUDENT)
            .build();

    promotionId = UUID.randomUUID();
    semesterId = UUID.randomUUID();
    courseId = UUID.randomUUID();
    examId = UUID.randomUUID();
  }

  private void mockPromotionResolution() {
    var promotion = hei.school.graduation.entity.PromotionEntity.builder().id(promotionId).build();
    var semester =
        SemesterEntity.builder()
            .id(semesterId)
            .promotion(promotion)
            .number(1)
            .cursusYear(1)
            .build();
    var assignment =
        StudentGroupAssignmentEntity.builder()
            .semestre(semester)
            .dateDebut(LocalDate.now())
            .build();

    when(studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(student.getId()))
        .thenReturn(List.of(assignment));
  }

  @Test
  void generate_shouldProduceNonEmptyPdf_withCourseAndExamDetails() throws Exception {
    mockPromotionResolution();

    var promotion = hei.school.graduation.entity.PromotionEntity.builder().id(promotionId).build();
    var semester =
        SemesterEntity.builder()
            .id(semesterId)
            .promotion(promotion)
            .number(1)
            .cursusYear(1)
            .build();

    for (int year = 1; year <= 3; year++) {
      if (year == 1) {
        when(semesterRepository.findByPromotion_IdAndCursusYear(promotionId, year))
            .thenReturn(List.of(semester));
      } else {
        when(semesterRepository.findByPromotion_IdAndCursusYear(promotionId, year))
            .thenReturn(List.of());
      }
      when(annualAverageCalculator.compute(student.getId(), year))
          .thenReturn(
              new AnnualAverageCalculator.AnnualAverageResult(
                  year == 1 ? new BigDecimal("14.32") : BigDecimal.ZERO,
                  year == 1 ? 5 : 0,
                  year == 1));
    }

    when(groupIsolationService.resolveFollowedCourseIdsIfAssigned(student.getId(), semesterId))
        .thenReturn(List.of(courseId));

    CourseEntity course =
        CourseEntity.builder()
            .id(courseId)
            .referenceCs("PROG1")
            .title("Programmation 1")
            .credits(5)
            .build();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    ExamEntity exam =
        ExamEntity.builder()
            .id(examId)
            .course(course)
            .examDate(LocalDate.of(2026, 1, 15))
            .coefficient(new BigDecimal("1.00"))
            .type(ExamType.NORMAL)
            .build();
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam));

    NoteEntity note =
        NoteEntity.builder().exam(exam).student(student).value(new BigDecimal("15.50")).build();
    when(noteRepository.findByStudent_IdAndExam_IdIn(student.getId(), List.of(examId)))
        .thenReturn(List.of(note));

    when(courseAverageCalculator.compute(courseId, student.getId()))
        .thenReturn(
            new CourseAverageCalculator.CourseAverageResult(
                new BigDecimal("15.50"), StatutReleve.COMPLET));

    File pdfFile = generator.generate(student);

    assertThat(pdfFile).exists();
    assertThat(pdfFile.length()).isGreaterThan(0);
    pdfFile.deleteOnExit();
  }

  @Test
  void generate_shouldThrowNotFound_whenStudentHasNoGroupAssignmentAtAll() {
    when(studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(student.getId()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> generator.generate(student))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(student.getId().toString());
  }
}
