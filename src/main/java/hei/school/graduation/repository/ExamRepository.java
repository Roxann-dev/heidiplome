package hei.school.graduation.repository;

import hei.school.graduation.entity.ExamEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;

public interface ExamRepository extends JpaRepository<ExamEntity, UUID> {
    List<ExamEntity> findByCourseId(UUID courseId);
}
