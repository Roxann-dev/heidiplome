package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.PromotionEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.PromotionMapper;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.service.PromotionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

  @Mock private PromotionRepository promotionRepository;
  @Mock private PromotionMapper promotionMapper;

  private PromotionService service;

  private UUID promotionId;
  private PromotionEntity entity;

  @BeforeEach
  void setUp() {
    service = new PromotionService(promotionRepository, promotionMapper);
    promotionId = UUID.randomUUID();
    entity =
        PromotionEntity.builder().id(promotionId).label("Promotion 2023").entryYear(2023).build();
  }

  @Test
  void findAll_shouldReturnMappedList() {
    var domain = new hei.school.graduation.model.Promotion(promotionId, "Promotion 2023", 2023);

    when(promotionRepository.findAll()).thenReturn(List.of(entity));
    when(promotionMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findAll();

    assertThat(result).containsExactly(domain);
  }

  @Test
  void findAll_shouldReturnEmptyList_whenNoPromotionExists() {
    when(promotionRepository.findAll()).thenReturn(List.of());

    var result = service.findAll();

    assertThat(result).isEmpty();
  }

  @Test
  void findById_shouldReturnMappedPromotion_whenExists() {
    var domain = new hei.school.graduation.model.Promotion(promotionId, "Promotion 2023", 2023);

    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(entity));
    when(promotionMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findById(promotionId);

    assertThat(result).isEqualTo(domain);
  }

  @Test
  void findById_shouldThrowNotFound_whenPromotionDoesNotExist() {
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(promotionId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(promotionId.toString());
  }
}
