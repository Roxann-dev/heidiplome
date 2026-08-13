package hei.school.graduation.mapper;

import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.model.TeacherCourseAssignment;
import org.springframework.stereotype.Component;

@Component
public class TeacherCourseAssignmentMapper {

  public TeacherCourseAssignment toDomain(TeacherCourseAssignmentEntity entity) {
    if (entity == null) {
      return null;
    }
    return new TeacherCourseAssignment(
        entity.getId(), entity.getTeacherId(), entity.getCourseId(), entity.getAnneeAcademique());
  }

  public TeacherCourseAssignmentEntity toEntity(TeacherCourseAssignment assignment) {
    if (assignment == null) {
      return null;
    }
    return TeacherCourseAssignmentEntity.builder()
        .id(assignment.id())
        .teacherId(assignment.teacherId())
        .courseId(assignment.courseId())
        .anneeAcademique(assignment.anneeAcademique())
        .build();
  }
}
