package hei.school.graduation.repository;

import hei.school.graduation.entity.AcademicGroupEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicGroupRepository extends JpaRepository<AcademicGroupEntity, UUID> {
  List<AcademicGroupEntity> findBySemester_Id(UUID semesterId);
}
