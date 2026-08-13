package hei.school.graduation.repository;

import hei.school.graduation.Entity.PromotionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {}
