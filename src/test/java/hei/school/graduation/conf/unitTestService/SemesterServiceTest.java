package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.SemesterMapper;
import hei.school.graduation.model.Semester;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.service.SemesterService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SemesterServiceTest {

  @Mock private SemesterRepository semesterRepository;
  @Mock private PromotionRepository promotionRepository;
  @Mock private SemesterMapper semesterMapper;

  private SemesterService service;

  private UUID promotionId;

  @BeforeEach
  void setUp() {
    service = new SemesterService(semesterRepository, promotionRepository, semesterMapper);
    promotionId = UUID.randomUUID();
  }

  @Test
  void findByPromotion_shouldReturnMappedList_whenPromotionExists() {
    UUID semesterId = UUID.randomUUID();
    SemesterEntity entity = SemesterEntity.builder().id(semesterId).build();
    Semester domain = new Semester(semesterId, promotionId, 1, 1);

    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(entity));
    when(semesterMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findByPromotion(promotionId);

    assertThat(result).containsExactly(domain);
  }

  @Test
  void findByPromotion_shouldReturnEmptyList_whenPromotionHasNoSemesters() {
    when(promotionRepository.existsById(promotionId)).thenReturn(true);
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId)).thenReturn(List.of());

    var result = service.findByPromotion(promotionId);

    assertThat(result).isEmpty();
  }

  @Test
  void findByPromotion_shouldThrowNotFound_whenPromotionDoesNotExist() {
    when(promotionRepository.existsById(promotionId)).thenReturn(false);

    assertThatThrownBy(() -> service.findByPromotion(promotionId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(promotionId.toString());
  }
}
