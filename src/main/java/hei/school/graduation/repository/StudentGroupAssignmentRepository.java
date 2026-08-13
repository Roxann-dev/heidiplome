package hei.school.graduation.repository;

import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGroupAssignmentRepository
    extends JpaRepository<StudentGroupAssignmentEntity, UUID> {

  List<StudentGroupAssignmentEntity> findByStudentIdOrderByDateDebutAsc(UUID studentId);

  List<StudentGroupAssignmentEntity> findByGroupId(UUID groupId);

  boolean existsByStudentIdAndSemestreId(UUID studentId, UUID semestreId);
}
