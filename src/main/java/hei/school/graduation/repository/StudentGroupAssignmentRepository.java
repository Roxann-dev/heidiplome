package hei.school.graduation.repository;

import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.model.Enum.Parcours;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGroupAssignmentRepository
    extends JpaRepository<StudentGroupAssignmentEntity, UUID> {

  Optional<StudentGroupAssignmentEntity> findByStudent_IdAndSemestre_Id(
      UUID studentId, UUID semestreId);

  List<StudentGroupAssignmentEntity> findByStudent_IdOrderByDateDebutAsc(UUID studentId);

  List<StudentGroupAssignmentEntity> findByGroup_Id(UUID groupId);

  boolean existsByStudent_IdAndSemestre_Id(UUID studentId, UUID semestreId);

  List<StudentGroupAssignmentEntity> findBySemestre_IdAndGroup_Parcours(
      UUID semestreId, Parcours parcours);
}
