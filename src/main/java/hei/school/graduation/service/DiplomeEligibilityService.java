package hei.school.graduation.service;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiplomeEligibilityService {

  private final SemesterRepository semesterRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final GroupIsolationService groupIsolationService;
  private final CourseAverageCalculator courseAverageCalculator;
  private final CourseRepository courseRepository;

  public record EligibilityResult(boolean diplome, BigDecimal moyenneCumulee) {}

  public EligibilityResult evaluate(UUID studentId, UUID promotionId) {
    List<SemesterEntity> semesters =
        semesterRepository.findByPromotion_IdOrderByNumeroAsc(promotionId);

    Set<UUID> coursesSuivis = new HashSet<>();

    for (SemesterEntity semester : semesters) {
      boolean studentAAtteintCeSemestre =
          studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(
              studentId, semester.getId());

      if (!studentAAtteintCeSemestre) {
        continue;
      }

      coursesSuivis.addAll(
          groupIsolationService.resolveFollowedCourseIds(studentId, semester.getId()));
    }

    if (coursesSuivis.isEmpty()) {
      return new EligibilityResult(false, BigDecimal.ZERO);
    }

    boolean diplome = true;
    BigDecimal sommePonderee = BigDecimal.ZERO;
    int totalCredits = 0;

    for (UUID courseId : coursesSuivis) {
      var result = courseAverageCalculator.compute(courseId, studentId);

      if (result.statut() != StatutReleve.COMPLET) {
        diplome = false;
        continue;
      }

      if (result.moyenne().compareTo(BigDecimal.TEN) < 0) {
        diplome = false;
      }

      CourseEntity course =
          courseRepository
              .findById(courseId)
              .orElseThrow(() -> new NotFoundException("Course introuvable : " + courseId));

      sommePonderee =
          sommePonderee.add(result.moyenne().multiply(BigDecimal.valueOf(course.getCredits())));
      totalCredits += course.getCredits();
    }

    BigDecimal moyenneCumulee =
        totalCredits == 0
            ? BigDecimal.ZERO
            : sommePonderee.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);

    return new EligibilityResult(diplome, moyenneCumulee);
  }
}
