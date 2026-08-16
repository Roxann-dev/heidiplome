package hei.school.graduation.service;

import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.PromotionMapper;
import hei.school.graduation.model.Promotion;
import hei.school.graduation.repository.PromotionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromotionService {

  private final PromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper;

  public List<Promotion> findAll() {
    return promotionRepository.findAll().stream().map(promotionMapper::toDomain).toList();
  }

  public Promotion findById(UUID promotionId) {
    return promotionRepository
        .findById(promotionId)
        .map(promotionMapper::toDomain)
        .orElseThrow(() -> new NotFoundException("Promotion introuvable : " + promotionId));
  }
}
