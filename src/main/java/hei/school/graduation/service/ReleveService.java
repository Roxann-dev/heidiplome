package hei.school.graduation.service;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.CourseNoteLine;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.ReleveSemester;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.security.UserPrincipal;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReleveService {

  private final CourseRepository courseRepository;
  private final GroupIsolationService groupIsolationService;
  private final CourseAverageCalculator courseAverageCalculator;

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
