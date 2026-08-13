package hei.school.graduation.repository;

import hei.school.graduation.entity.SemesterEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<SemesterEntity, UUID> {}
