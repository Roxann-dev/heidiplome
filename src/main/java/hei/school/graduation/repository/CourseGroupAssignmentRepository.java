package hei.school.graduation.repository;

import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseGroupAssignmentRepository
    extends JpaRepository<CourseGroupAssignmentEntity, UUID> {

  List<CourseGroupAssignmentEntity> findByGroup_Id(UUID groupId);

  List<CourseGroupAssignmentEntity> findByCourse_Id(UUID courseId);

  boolean existsByCourse_IdAndGroup_Id(UUID courseId, UUID groupId);
}
