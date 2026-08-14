package hei.school.graduation.repository;

import hei.school.graduation.entity.ExamEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<ExamEntity, UUID> {
  List<ExamEntity> findByCourseId(UUID courseId);
}
