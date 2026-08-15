package hei.school.graduation.service;

import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiplomeEligibilityService {

  private final SemesterRepository semestreRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final GroupIsolationService groupIsolationService;
  private final CourseAverageCalculator courseAverageCalculator;

  public boolean isDiplome(UUID studentId, UUID promotionId) {
    List<SemesterEntity> semestres =
        semestreRepository.findByPromotion_IdOrderByNumeroAsc(promotionId);

    Set<UUID> coursesSuivis = new HashSet<>();

    for (SemesterEntity semestre : semestres) {
      boolean studentAAtteintCeSemestre =
          studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(
              studentId, semestre.getId());

      if (!studentAAtteintCeSemestre) {
        continue;
      }

      coursesSuivis.addAll(
          groupIsolationService.resolveFollowedCourseIds(studentId, semestre.getId()));
    }

    if (coursesSuivis.isEmpty()) {
      return false;
    }

    for (UUID courseId : coursesSuivis) {
      var result = courseAverageCalculator.compute(courseId, studentId);

      if (result.statut() != StatutReleve.COMPLET) {
        return false;
      }
      if (result.moyenne().compareTo(BigDecimal.TEN) < 0) {
        return false;
      }
    }

    return true;
  }
}
