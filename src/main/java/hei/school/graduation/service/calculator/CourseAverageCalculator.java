package hei.school.graduation.service.calculator;

import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAverageCalculator {

  private final ExamRepository examRepository;
  private final NoteRepository noteRepository;

  public record CourseAverageResult(BigDecimal moyenne, StatutReleve statut) {}

  public CourseAverageResult compute(UUID courseId, UUID studentId) {
    List<ExamEntity> exams = examRepository.findByCourseId(courseId);

    List<ExamEntity> normaux = exams.stream().filter(e -> e.getType() == ExamType.NORMAL).toList();

    BigDecimal sommeCoefficients =
        normaux.stream().map(ExamEntity::getCoefficient).reduce(BigDecimal.ZERO, BigDecimal::add);

    boolean courseCloture = sommeCoefficients.compareTo(BigDecimal.ONE) == 0;

    if (!courseCloture) {
      return new CourseAverageResult(null, StatutReleve.PROVISOIRE);
    }

    List<UUID> examIds = exams.stream().map(ExamEntity::getId).toList();
    List<NoteEntity> notes = noteRepository.findByStudentIdAndExamenIdIn(studentId, examIds);

    Map<UUID, BigDecimal> valeurParExam =
        notes.stream().collect(Collectors.toMap(n -> n.getExam().getId(), NoteEntity::getValue));

    boolean toutesLesNotesNormalesPresentes =
        normaux.stream().allMatch(e -> valeurParExam.containsKey(e.getId()));

    if (!toutesLesNotesNormalesPresentes) {
      return new CourseAverageResult(null, StatutReleve.PROVISOIRE);
    }

    BigDecimal moyenneNormale =
        normaux.stream()
            .map(e -> valeurParExam.get(e.getId()).multiply(e.getCoefficient()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal moyenneFinale = moyenneNormale;

    if (moyenneNormale.compareTo(BigDecimal.TEN) < 0) {
      ExamEntity rattrapage =
          exams.stream().filter(e -> e.getType() == ExamType.RATTRAPAGE).findFirst().orElse(null);

      if (rattrapage != null && valeurParExam.containsKey(rattrapage.getId())) {
        BigDecimal noteRattrapage = valeurParExam.get(rattrapage.getId());
        moyenneFinale = noteRattrapage.min(BigDecimal.TEN);
      }
    }

    return new CourseAverageResult(
        moyenneFinale.setScale(2, RoundingMode.HALF_UP), StatutReleve.COMPLET);
  }
}
