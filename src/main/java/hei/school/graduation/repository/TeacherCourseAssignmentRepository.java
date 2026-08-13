package hei.school.graduation.repository;

import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherCourseAssignmentRepository
    extends JpaRepository<TeacherCourseAssignmentEntity, UUID> {

  List<TeacherCourseAssignmentEntity> findByTeacherId(UUID teacherId);

  List<TeacherCourseAssignmentEntity> findByTeacherIdAndAnneeAcademique(
      UUID teacherId, int anneeAcademique);

  boolean existsByTeacherIdAndCourseIdAndAnneeAcademique(
      UUID teacherId, UUID courseId, int anneeAcademique);

  boolean existsByTeacherIdAndCourseId(UUID teacherId, UUID courseId);
}
