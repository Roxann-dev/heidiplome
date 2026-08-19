package hei.school.graduation.service;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.CourseNoteLine;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.ReleveAnnuel;
import hei.school.graduation.model.ReleveSemester;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.security.UserPrincipal;
import hei.school.graduation.service.calculator.AnnualAverageCalculator;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReleveService {

  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final GroupIsolationService groupIsolationService;
  private final CourseAverageCalculator courseAverageCalculator;
  private final AnnualAverageCalculator annualAverageCalculator;

  public ReleveSemester getReleveSemestre(UUID studentId, UUID semestreId) {
    checkOwnershipOrAdmin(studentId);

    UUID groupId = groupIsolationService.resolveGroupId(studentId, semestreId);
    List<UUID> courseIds = groupIsolationService.resolveFollowedCourseIds(studentId, semestreId);

    List<CourseNoteLine> lines = courseIds.stream().map(id -> buildLine(id, studentId)).toList();

    StatutReleve globalStatus =
        lines.stream().allMatch(l -> l.status() == StatutReleve.COMPLET)
            ? StatutReleve.COMPLET
            : StatutReleve.PROVISOIRE;

    return new ReleveSemester(studentId, semestreId, groupId, lines, globalStatus);
  }

  public ReleveAnnuel getReleveAnnuel(UUID studentId, int anneeCursus) {
    checkOwnershipOrAdmin(studentId);

    UUID promotionId = resolvePromotionId(studentId);
    List<SemesterEntity> semesters =
        semesterRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus);

    List<CourseNoteLine> lines = new ArrayList<>();
    for (SemesterEntity semester : semesters) {
      List<UUID> courseIds =
          groupIsolationService.resolveFollowedCourseIds(studentId, semester.getId());
      for (UUID courseId : courseIds) {
        lines.add(buildLine(courseId, studentId));
      }
    }

    var aggregate = annualAverageCalculator.compute(studentId, anneeCursus);

    return new ReleveAnnuel(
        studentId, anneeCursus, aggregate.moyenneGenerale(), aggregate.totalCredits(), lines);
  }

  private CourseNoteLine buildLine(UUID courseId, UUID studentId) {
    var result = courseAverageCalculator.compute(courseId, studentId);
    CourseEntity course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

    return new CourseNoteLine(
        courseId,
        course.getReferenceCs(),
        course.getTitle(),
        course.getCredits(),
        result.moyenne(),
        result.statut());
  }

  private UUID resolvePromotionId(UUID studentId) {
    StudentGroupAssignmentEntity any =
        studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(studentId).stream()
            .findFirst()
            .orElseThrow(
                () -> new NotFoundException("No group assignment for student " + studentId));
    return any.getSemestre().getPromotion().getId();
  }

  private void checkOwnershipOrAdmin(UUID studentId) {
    UserPrincipal principal =
        (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    boolean isOwner =
        principal.getRole() == UserRole.STUDENT && principal.getId().equals(studentId);
    boolean isAdmin = principal.getRole() == UserRole.ADMIN;

    if (!isOwner && !isAdmin) {
      throw new AccessDeniedException("You can only view your own transcript.");
    }
  }
}
