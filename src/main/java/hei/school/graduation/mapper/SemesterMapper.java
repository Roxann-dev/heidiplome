package hei.school.graduation.mapper;

import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.model.Semester;
import org.springframework.stereotype.Component;

@Component
public class SemesterMapper {

  public Semester toDomain(SemesterEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Semester(
        entity.getId(), entity.getPromotion().getId(), entity.getNumber(), entity.getCursusYear());
  }

  public SemesterEntity toEntity(Semester semester) {
    if (semester == null) {
      return null;
    }
    return SemesterEntity.builder()
        .id(semester.id())
        .promotion(PromotionEntity.builder().id(semester.promotionId()).build())
        .number(semester.number())
        .cursusYear(semester.cursusYear())
        .build();
  }
}
