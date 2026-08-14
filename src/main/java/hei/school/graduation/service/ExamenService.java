package hei.school.graduation.service;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ExamenService {

  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;

  public ExamEntity create(
      UUID courseId, LocalDate dateExamen, BigDecimal coefficient, ExamType type) {
    CourseEntity course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

    ExamType resolvedType = type != null ? type : ExamType.NORMAL;

    if (resolvedType == ExamType.NORMAL) {
      BigDecimal existingSum =
          examRepository.findByCourseId(courseId).stream()
              .filter(examen -> examen.getType() == ExamType.NORMAL)
              .map(ExamEntity::getCoefficient)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal projectedSum = existingSum.add(coefficient);
      if (projectedSum.compareTo(BigDecimal.ONE) > 0) {
        throw new BadRequestException(
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
}
