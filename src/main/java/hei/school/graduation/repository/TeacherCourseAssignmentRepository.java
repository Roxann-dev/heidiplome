package hei.school.graduation.repository;

import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherCourseAssignmentRepository
    extends JpaRepository<TeacherCourseAssignmentEntity, UUID> {

  List<TeacherCourseAssignmentEntity> findByTeacher_Id(UUID teacherId);

  List<TeacherCourseAssignmentEntity> findByTeacher_IdAndAnneeAcademique(
      UUID teacherId, int anneeAcademique);

  boolean existsByTeacher_IdAndCourse_IdAndAnneeAcademique(
      UUID teacherId, UUID courseId, int anneeAcademique);

  boolean existsByTeacher_IdAndCourse_Id(UUID teacherId, UUID courseId);

  List<TeacherCourseAssignmentEntity> findByAnneeAcademique(int anneeAcademique);
}
