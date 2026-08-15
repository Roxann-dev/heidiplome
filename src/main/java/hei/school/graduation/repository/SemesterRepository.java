package hei.school.graduation.repository;

import hei.school.graduation.entity.SemesterEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<SemesterEntity, UUID> {

  List<SemesterEntity> findByPromotion_IdAndCursusYear(UUID promotionId, int cursusYear);

  List<SemesterEntity> findByPromotion_IdOrderByNumberAsc(UUID promotionId);

  Optional<SemesterEntity> findByPromotion_IdAndNumber(UUID promotionId, int number);
}
