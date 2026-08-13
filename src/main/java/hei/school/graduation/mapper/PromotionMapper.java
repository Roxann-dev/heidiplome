package hei.school.graduation.mapper;

import hei.school.graduation.Entity.PromotionEntity;
import hei.school.graduation.model.Promotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

  public Promotion toDomain(PromotionEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Promotion(entity.getId(), entity.getLabel(), entity.getEntryYear());
  }

  public PromotionEntity toEntity(Promotion promotion) {
    if (promotion == null) {
      return null;
    }
    return PromotionEntity.builder()
        .id(promotion.id())
        .label(promotion.label())
        .entryYear(promotion.entryYear())
        .build();
  }
}
