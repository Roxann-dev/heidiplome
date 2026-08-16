package hei.school.graduation.service;

import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.SemesterMapper;
import hei.school.graduation.model.Semester;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemesterService {

  private final SemesterRepository semesterRepository;
  private final PromotionRepository promotionRepository;
  private final SemesterMapper semesterMapper;

  public List<Semester> findByPromotion(UUID promotionId) {
    if (!promotionRepository.existsById(promotionId)) {
      throw new NotFoundException("Promotion introuvable : " + promotionId);
    }
    return semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId).stream()
        .map(semesterMapper::toDomain)
        .toList();
  }
}
