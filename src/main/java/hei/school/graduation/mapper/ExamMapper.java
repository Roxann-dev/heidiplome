package hei.school.graduation.mapper;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.model.Exam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

  public Exam toDomain(ExamEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Exam(
        entity.getId(),
        entity.getCourse().getId(),
        entity.getExamDate(),
        entity.getCoefficient(),
        entity.getType());
  }

  public ExamEntity toEntity(Exam exam) {
    if (exam == null) {
      return null;
    }
    return ExamEntity.builder()
        .id(exam.id())
        .course(CourseEntity.builder().id(exam.courseId()).build())
        .examDate(exam.examDate())
        .coefficient(exam.coefficient())
        .type(exam.type())
        .build();
  }
}
