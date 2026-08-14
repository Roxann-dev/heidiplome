package hei.school.graduation.mapper;

import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentGroupAssignmentMapper {

  private final UserRepository userRepository;
  private final AcademicGroupRepository academicGroupRepository;
  private final SemesterRepository semestreRepository;

  public StudentGroupAssignment toDomain(StudentGroupAssignmentEntity entity) {
    if (entity == null) {
      return null;
    }
    return new StudentGroupAssignment(
        entity.getId(),
        entity.getStudent().getId(),
        entity.getGroup().getId(),
        entity.getSemestre().getId(),
        entity.getDateDebut(),
        entity.getDateFin());
  }

  public StudentGroupAssignmentEntity toEntity(StudentGroupAssignment assignment) {
    if (assignment == null) {
      return null;
    }
    UserEntity student = userRepository.getReferenceById(assignment.studentId());
    AcademicGroupEntity group = academicGroupRepository.getReferenceById(assignment.groupId());
    SemesterEntity semestre = semestreRepository.getReferenceById(assignment.semestreId());

    return StudentGroupAssignmentEntity.builder()
        .id(assignment.id())
        .student(student)
        .group(group)
        .semestre(semestre)
        .dateDebut(assignment.dateDebut())
        .dateFin(assignment.dateFin())
        .build();
  }
}
