package hei.school.graduation.mapper;

import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.model.CourseGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseGroupAssignmentMapper {

  private final CourseRepository courseRepository;
  private final AcademicGroupRepository academicGroupRepository;
  private final SemesterRepository semesterRepository;

  public CourseGroupAssignment toDomain(CourseGroupAssignmentEntity entity) {
    if (entity == null) {
      return null;
    }
    return new CourseGroupAssignment(
        entity.getId(),
        entity.getCourse().getId(),
        entity.getGroup().getId(),
        entity.getSemestre().getId());
  }

  public CourseGroupAssignmentEntity toEntity(CourseGroupAssignment assignment) {
    if (assignment == null) {
      return null;
    }
    CourseEntity course = courseRepository.getReferenceById(assignment.courseId());
    AcademicGroupEntity group = academicGroupRepository.getReferenceById(assignment.groupId());
    SemesterEntity semestre = semesterRepository.getReferenceById(assignment.semestreId());

    return CourseGroupAssignmentEntity.builder()
        .id(assignment.id())
        .course(course)
        .group(group)
        .semestre(semestre)
        .build();
  }
}
