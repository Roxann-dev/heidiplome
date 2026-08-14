package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAverageCalculatorTest {

  @Mock private ExamRepository examRepository;
  @Mock private NoteRepository noteRepository;

  private CourseAverageCalculator calculator;

  private UUID courseId;
  private UUID studentId;

  @BeforeEach
  void setUp() {
    calculator = new CourseAverageCalculator(examRepository, noteRepository);
    courseId = UUID.randomUUID();
    studentId = UUID.randomUUID();
  }

  private ExamEntity exam(BigDecimal coefficient, ExamType type) {
    return ExamEntity.builder()
        .id(UUID.randomUUID())
        .examDate(LocalDate.now())
        .coefficient(coefficient)
        .type(type)
        .build();
  }

  private NoteEntity note(ExamEntity exam, BigDecimal value) {
    return NoteEntity.builder().id(UUID.randomUUID()).exam(exam).value(value).build();
  }

  @Test
  void shouldReturnProvisoire_whenCourseIsNotClosed() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    // pas d'exam2 : somme = 0.50, pas encore clôturé

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.PROVISOIRE);
    assertThat(result.moyenne()).isNull();
  }

  @Test
  void shouldReturnProvisoire_whenStudentIsMissingANormalNote() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity exam2 = exam(new BigDecimal("0.50"), ExamType.NORMAL);

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1, exam2));
    // seule la note de exam1 existe, exam2 n'a pas encore été saisi
    when(noteRepository.findByStudent_IdAndExam_IdIn(eq(studentId), any()))
        .thenReturn(List.of(note(exam1, new BigDecimal("12.00"))));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.PROVISOIRE);
    assertThat(result.moyenne()).isNull();
  }

  @Test
  void shouldReturnNormalAverage_whenAboveTen_evenIfRattrapageExists() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity exam2 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity rattrapage = exam(BigDecimal.ONE, ExamType.RATTRAPAGE);

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1, exam2, rattrapage));
    when(noteRepository.findByStudent_IdAndExam_IdIn(eq(studentId), any()))
        .thenReturn(
            List.of(note(exam1, new BigDecimal("12.00")), note(exam2, new BigDecimal("14.00"))));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.COMPLET);
    assertThat(result.moyenne()).isEqualByComparingTo("13.00");
  }

  @Test
  void shouldReturnNormalAverage_whenBelowTenAndNoRattrapageExam() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity exam2 = exam(new BigDecimal("0.50"), ExamType.NORMAL);

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1, exam2));
    when(noteRepository.findByStudent_IdAndExam_IdIn(eq(studentId), any()))
        .thenReturn(
            List.of(note(exam1, new BigDecimal("8.00")), note(exam2, new BigDecimal("6.00"))));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.COMPLET);
    assertThat(result.moyenne()).isEqualByComparingTo("7.00");
  }

  @Test
  void shouldReturnNormalAverage_whenBelowTenAndRattrapageExistsButNotTaken() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity exam2 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity rattrapage = exam(BigDecimal.ONE, ExamType.RATTRAPAGE);

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1, exam2, rattrapage));
    when(noteRepository.findByStudent_IdAndExam_IdIn(eq(studentId), any()))
        .thenReturn(
            List.of(note(exam1, new BigDecimal("8.00")), note(exam2, new BigDecimal("6.00"))));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.COMPLET);
    assertThat(result.moyenne()).isEqualByComparingTo("7.00");
  }

  @Test
  void shouldUseRattrapageNote_whenBelowTenAndRattrapageTaken() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity exam2 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity rattrapage = exam(BigDecimal.ONE, ExamType.RATTRAPAGE);

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1, exam2, rattrapage));
    when(noteRepository.findByStudent_IdAndExam_IdIn(eq(studentId), any()))
        .thenReturn(
            List.of(
                note(exam1, new BigDecimal("8.00")),
                note(exam2, new BigDecimal("6.00")),
                note(rattrapage, new BigDecimal("9.00"))));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.COMPLET);
    assertThat(result.moyenne()).isEqualByComparingTo("9.00");
  }

  @Test
  void shouldCapRattrapageNoteAtTen_whenRattrapageAboveTen() {
    ExamEntity exam1 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity exam2 = exam(new BigDecimal("0.50"), ExamType.NORMAL);
    ExamEntity rattrapage = exam(BigDecimal.ONE, ExamType.RATTRAPAGE);

    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam1, exam2, rattrapage));
    when(noteRepository.findByStudent_IdAndExam_IdIn(eq(studentId), any()))
        .thenReturn(
            List.of(
                note(exam1, new BigDecimal("8.00")),
                note(exam2, new BigDecimal("6.00")),
                note(rattrapage, new BigDecimal("15.00"))));

    var result = calculator.compute(courseId, studentId);

    assertThat(result.statut()).isEqualTo(StatutReleve.COMPLET);
    assertThat(result.moyenne()).isEqualByComparingTo("10.00");
  }
}
