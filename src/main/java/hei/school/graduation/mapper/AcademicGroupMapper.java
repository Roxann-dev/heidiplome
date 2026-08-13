package hei.school.graduation.mapper;

import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.model.AcademicGroup;
import org.springframework.stereotype.Component;

@Component
public class AcademicGroupMapper {

  public AcademicGroup toDomain(AcademicGroupEntity entity) {
    if (entity == null) {
      return null;
    }
    return new AcademicGroup(
        entity.getId(), entity.getReference(), entity.getParcours(), entity.getSemester().getId());
  }

  public AcademicGroupEntity toEntity(AcademicGroup academicGroup) {
    if (academicGroup == null) {
      return null;
    }
    return AcademicGroupEntity.builder()
        .id(academicGroup.id())
        .reference(academicGroup.reference())
        .parcours(academicGroup.parcours())
        .semester(SemesterEntity.builder().id(academicGroup.semesterId()).build())
        .build();
  }
}
