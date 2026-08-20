package hei.school.graduation.service.calculator;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.GroupIsolationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AnnualAverageCalculator {

  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final GroupIsolationService groupIsolationService;
  private final CourseAverageCalculator courseAverageCalculator;

  public record AnnualAverageResult(
      BigDecimal moyenneGenerale, int totalCredits, boolean complet) {}

  @Transactional(readOnly = true)
  public AnnualAverageResult compute(UUID studentId, int anneeCursus) {
    UUID promotionId = resolvePromotionId(studentId);
    List<SemesterEntity> semestres =
        semesterRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus);

    BigDecimal sommePonderee = BigDecimal.ZERO;
    int totalCredits = 0;
    boolean complet = true;

    for (SemesterEntity semestre : semestres) {
      List<UUID> courseIds =
              groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, semestre.getId());

      if (courseIds.isEmpty()) {
        complet = false;
        continue;
      }

      for (UUID courseId : courseIds) {
        var result = courseAverageCalculator.compute(courseId, studentId);

        if (result.statut() == StatutReleve.PROVISOIRE) {
          complet = false;
          continue;
        }

        CourseEntity course =
            courseRepository
                .findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course introuvable : " + courseId));

        sommePonderee =
            sommePonderee.add(result.moyenne().multiply(BigDecimal.valueOf(course.getCredits())));
        totalCredits += course.getCredits();
      }
    }

    BigDecimal moyenneGenerale =
        totalCredits == 0
            ? BigDecimal.ZERO
            : sommePonderee.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);

    return new AnnualAverageResult(moyenneGenerale, totalCredits, complet);
  }

  private UUID resolvePromotionId(UUID studentId) {
    StudentGroupAssignmentEntity anyAssignment =
        studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(studentId).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new NotFoundException("Aucune affectation de group pour student " + studentId));
    return anyAssignment.getSemestre().getPromotion().getId();
  }
}
