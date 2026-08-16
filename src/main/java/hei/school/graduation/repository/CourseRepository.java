package hei.school.graduation.repository;

import hei.school.graduation.entity.CourseEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {
    List<CourseEntity> findBySemester_Id(UUID semesterId);
}
