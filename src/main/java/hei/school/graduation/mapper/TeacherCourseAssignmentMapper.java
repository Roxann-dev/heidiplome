package hei.school.graduation.mapper;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.TeacherCourseAssignment;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherCourseAssignmentMapper {

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;

  public TeacherCourseAssignment toDomain(TeacherCourseAssignmentEntity entity) {
    if (entity == null) {
      return null;
    }
    return new TeacherCourseAssignment(
        entity.getId(),
        entity.getTeacher().getId(),
        entity.getCourse().getId(),
        entity.getAnneeAcademique());
  }

  public TeacherCourseAssignmentEntity toEntity(TeacherCourseAssignment assignment) {
    if (assignment == null) {
      return null;
    }
    UserEntity teacher = userRepository.getReferenceById(assignment.teacherId());
    CourseEntity course = courseRepository.getReferenceById(assignment.courseId());

    return TeacherCourseAssignmentEntity.builder()
        .id(assignment.id())
        .teacher(teacher)
        .course(course)
        .anneeAcademique(assignment.anneeAcademique())
        .build();
  }
}
