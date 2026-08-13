package hei.school.graduation.repository;

import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseGroupAssignmentRepository
    extends JpaRepository<CourseGroupAssignmentEntity, UUID> {

  List<CourseGroupAssignmentEntity> findByGroupId(UUID groupId);

  List<CourseGroupAssignmentEntity> findByCourseId(UUID courseId);

  boolean existsByCourseIdAndGroupId(UUID courseId, UUID groupId);
}
