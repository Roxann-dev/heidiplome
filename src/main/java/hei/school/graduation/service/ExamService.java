package hei.school.graduation.service;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ExamService {

  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;

  public ExamEntity create(
          UUID courseId, LocalDate dateExamen, BigDecimal coefficient, ExamType type) {
    CourseEntity course =
            courseRepository
                    .findById(courseId)
                    .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

    checkTeacherOwnsCourseOrAdmin(courseId);

    ExamType resolvedType = type != null ? type : ExamType.NORMAL;

    if (resolvedType == ExamType.NORMAL) {
      BigDecimal existingSum =
              examRepository.findByCourseId(courseId).stream()
                      .filter(examen -> examen.getType() == ExamType.NORMAL)
                      .map(ExamEntity::getCoefficient)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal projectedSum = existingSum.add(coefficient);
      if (projectedSum.compareTo(BigDecimal.ONE) > 0) {
        throw new ConflictException(
                "Sum of NORMAL exam coefficients for course "
                        + courseId
                        + " would exceed 1 (currently "
                        + existingSum
                        + ", adding "
                        + coefficient
                        + ")");
      }
    }

    ExamEntity examen =
            ExamEntity.builder()
                    .course(course)
                    .examDate(dateExamen)
                    .coefficient(coefficient)
                    .type(resolvedType)
                    .build();

    return examRepository.save(examen);
  }

  public List<ExamEntity> findByCourse(UUID courseId) {
    if (!courseRepository.existsById(courseId)) {
      throw new NotFoundException("Course not found: " + courseId);
    }
    return examRepository.findByCourseId(courseId);
  }

  public ExamEntity findById(UUID examId) {
    return examRepository
            .findById(examId)
            .orElseThrow(() -> new NotFoundException("Exam not found: " + examId));
  }

  private void checkTeacherOwnsCourseOrAdmin(UUID courseId) {
    UserPrincipal principal =
            (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    boolean isAdmin = principal.getRole() == UserRole.ADMIN;
    boolean isAssignedTeacher =
            principal.getRole() == UserRole.TEACHER
                    && teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(
                    principal.getId(), courseId);

    if (!isAdmin && !isAssignedTeacher) {
      throw new AccessDeniedException("You can only create exams for your own courses.");
    }
  }
}