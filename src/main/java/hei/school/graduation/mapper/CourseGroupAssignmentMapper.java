package hei.school.graduation.mapper;

import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.model.CourseGroupAssignment;
import org.springframework.stereotype.Component;

@Component
public class CourseGroupAssignmentMapper {

  public CourseGroupAssignment toDomain(CourseGroupAssignmentEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CourseGroupAssignment(
        entity.getId(), entity.getCourseId(), entity.getGroupId(), entity.getSemestreId());
  }

  public CourseGroupAssignmentEntity toEntity(CourseGroupAssignment assignment) {
    if (assignment == null) {
      return null;
    }
    return CourseGroupAssignmentEntity.builder()
        .id(assignment.id())
        .courseId(assignment.courseId())
        .groupId(assignment.groupId())
        .semestreId(assignment.semestreId())
        .build();
  }
}
