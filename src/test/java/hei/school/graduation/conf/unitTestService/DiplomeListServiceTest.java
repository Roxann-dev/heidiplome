package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.DiplomeEligibilityService;
import hei.school.graduation.service.DiplomeListService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiplomeListServiceTest {

  @Mock private SemesterRepository semesterRepository;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private DiplomeEligibilityService diplomeEligibilityService;

  private DiplomeListService service;

  private UUID promotionId;
  private UUID semestreS6Id;

  @BeforeEach
  void setUp() {
    service =
        new DiplomeListService(
            semesterRepository, studentGroupAssignmentRepository, diplomeEligibilityService);
    promotionId = UUID.randomUUID();
    semestreS6Id = UUID.randomUUID();
  }

  private UserEntity student(String reference, String nom, String prenom) {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .reference(reference)
        .lastName(nom)
        .firstName(prenom)
        .build();
  }

  @Test
  void computeDiplomes_shouldReturnRankedListSortedByAverageDescending() {
    SemesterEntity s6 = SemesterEntity.builder().id(semestreS6Id).build();
    when(semesterRepository.findByPromotion_IdAndNumber(promotionId, 6))
        .thenReturn(Optional.of(s6));

    UserEntity student1 = student("STD-001", "Rakoto", "Roxann");
    UserEntity student2 = student("STD-002", "Rabe", "Andry");

    StudentGroupAssignmentEntity assignment1 =
        StudentGroupAssignmentEntity.builder().student(student1).build();
    StudentGroupAssignmentEntity assignment2 =
        StudentGroupAssignmentEntity.builder().student(student2).build();

    when(studentGroupAssignmentRepository.findBySemestre_IdAndGroup_Parcours(
            semestreS6Id, Parcours.EL))
        .thenReturn(List.of(assignment1, assignment2));

    when(diplomeEligibilityService.evaluate(student1.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("12.00")));
    when(diplomeEligibilityService.evaluate(student2.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("16.00")));

    var result = service.computeDiplomes(promotionId, Parcours.EL);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).std()).isEqualTo("STD-002");
    assertThat(result.get(0).rang()).isEqualTo(1);
    assertThat(result.get(1).std()).isEqualTo("STD-001");
    assertThat(result.get(1).rang()).isEqualTo(2);
  }

  @Test
  void computeDiplomes_shouldExcludeStudentsNotEligible() {
    SemesterEntity s6 = SemesterEntity.builder().id(semestreS6Id).build();
    when(semesterRepository.findByPromotion_IdAndNumber(promotionId, 6))
        .thenReturn(Optional.of(s6));

    UserEntity student1 = student("STD-001", "Rakoto", "Roxann");
    StudentGroupAssignmentEntity assignment1 =
        StudentGroupAssignmentEntity.builder().student(student1).build();

    when(studentGroupAssignmentRepository.findBySemestre_IdAndGroup_Parcours(
            semestreS6Id, Parcours.TN))
        .thenReturn(List.of(assignment1));

    when(diplomeEligibilityService.evaluate(student1.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(false, BigDecimal.ZERO));

    var result = service.computeDiplomes(promotionId, Parcours.TN);

    assertThat(result).isEmpty();
  }

  @Test
  void computeDiplomes_shouldThrowNotFound_whenNoSemestreS6ForPromotion() {
    when(semesterRepository.findByPromotion_IdAndNumber(promotionId, 6))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.computeDiplomes(promotionId, Parcours.EL))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void computeDiplomes_shouldReturnEmptyList_whenNoStudentInParcours() {
    SemesterEntity s6 = SemesterEntity.builder().id(semestreS6Id).build();
    when(semesterRepository.findByPromotion_IdAndNumber(promotionId, 6))
        .thenReturn(Optional.of(s6));
    when(studentGroupAssignmentRepository.findBySemestre_IdAndGroup_Parcours(
            semestreS6Id, Parcours.EL))
        .thenReturn(List.of());

    var result = service.computeDiplomes(promotionId, Parcours.EL);

    assertThat(result).isEmpty();
  }
}
