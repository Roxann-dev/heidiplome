package hei.school.graduation.mapper;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.model.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public Course toDomain(CourseEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Course(
        entity.getId(),
        entity.getReferenceCs(),
        entity.getTitle(),
        entity.getCredits(),
        entity.getSemester().getId());
  }

  public CourseEntity toEntity(Course course) {
    if (course == null) {
      return null;
    }
    return CourseEntity.builder()
        .id(course.id())
        .referenceCs(course.referenceCs())
        .title(course.title())
        .credits(course.credits())
        .semester(SemesterEntity.builder().id(course.semesterId()).build())
        .build();
  }
}
