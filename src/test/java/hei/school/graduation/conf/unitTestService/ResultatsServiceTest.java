package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.model.Enum.StatutDiplome;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.ResultatStudent;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.DiplomeEligibilityService;
import hei.school.graduation.service.ResultatsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultatsServiceTest {

  @Mock private SemesterRepository semesterRepository;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private DiplomeEligibilityService diplomeEligibilityService;

  private ResultatsService service;

  private UUID promotionId;

  @BeforeEach
  void setUp() {
    service =
        new ResultatsService(
            semesterRepository, studentGroupAssignmentRepository, diplomeEligibilityService);
    promotionId = UUID.randomUUID();
  }

  private SemesterEntity semestre(int numero) {
    return SemesterEntity.builder().id(UUID.randomUUID()).number(numero).build();
  }

  private UserEntity student(String reference, String nom, String prenom) {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .reference(reference)
        .lastName(nom)
        .firstName(prenom)
        .role(UserRole.STUDENT)
        .build();
  }

  private StudentGroupAssignmentEntity assignment(UserEntity student, Parcours parcours) {
    AcademicGroupEntity group =
        AcademicGroupEntity.builder().id(UUID.randomUUID()).parcours(parcours).build();
    return StudentGroupAssignmentEntity.builder()
        .id(UUID.randomUUID())
        .student(student)
        .group(group)
        .dateDebut(LocalDate.now())
        .build();
  }

  @Test
  void computeResultats_shouldThrowNotFound_whenPromotionHasNoSemesters() {
    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId)).thenReturn(List.of());

    assertThatThrownBy(() -> service.computeResultats(promotionId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(promotionId.toString());
  }

  @Test
  void computeResultats_shouldReturnStatutEnCours_whenCursusNonTermine() {
    SemesterEntity s1 = semestre(1);
    UserEntity etudiant = student("STD-001", "Rakoto", "Jean");
    StudentGroupAssignmentEntity affectation = assignment(etudiant, null);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s1));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s1.getId()))
        .thenReturn(List.of(affectation));
    when(diplomeEligibilityService.evaluate(etudiant.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("14.00")));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(etudiant.getId()))
        .thenReturn(Optional.of(affectation));

    var result = service.computeResultats(promotionId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).statut()).isEqualTo(StatutDiplome.EN_COURS);
  }

  @Test
  void computeResultats_shouldReturnDiplome_whenCursusTermineEtEligible() {
    SemesterEntity s6 = semestre(6);
    UserEntity etudiant = student("STD-001", "Rakoto", "Jean");
    StudentGroupAssignmentEntity affectation = assignment(etudiant, Parcours.EL);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s6));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s6.getId()))
        .thenReturn(List.of(affectation));
    when(diplomeEligibilityService.evaluate(etudiant.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("12.50")));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(etudiant.getId()))
        .thenReturn(Optional.of(affectation));

    var result = service.computeResultats(promotionId);

    assertThat(result).hasSize(1);
    ResultatStudent resultat = result.get(0);
    assertThat(resultat.statut()).isEqualTo(StatutDiplome.DIPLOME);
    assertThat(resultat.parcoursActuel()).isEqualTo(Parcours.EL);
    assertThat(resultat.moyenneCumulee()).isEqualByComparingTo("12.50");
    assertThat(resultat.std()).isEqualTo("STD-001");
  }

  @Test
  void computeResultats_shouldReturnNonDiplome_whenCursusTermineEtNonEligible() {
    SemesterEntity s6 = semestre(6);
    UserEntity etudiant = student("STD-002", "Rasoa", "Marie");
    StudentGroupAssignmentEntity affectation = assignment(etudiant, Parcours.TN);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s6));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s6.getId()))
        .thenReturn(List.of(affectation));
    when(diplomeEligibilityService.evaluate(etudiant.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(false, new BigDecimal("8.20")));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(etudiant.getId()))
        .thenReturn(Optional.of(affectation));

    var result = service.computeResultats(promotionId);

    assertThat(result.get(0).statut()).isEqualTo(StatutDiplome.NON_DIPLOME);
  }

  @Test
  void computeResultats_shouldSetParcoursNull_whenNoCurrentAssignmentFound() {
    SemesterEntity s6 = semestre(6);
    UserEntity etudiant = student("STD-003", "Rabe", "Paul");
    StudentGroupAssignmentEntity affectation = assignment(etudiant, Parcours.EL);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s6));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s6.getId()))
        .thenReturn(List.of(affectation));
    when(diplomeEligibilityService.evaluate(etudiant.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, BigDecimal.TEN));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(etudiant.getId()))
        .thenReturn(Optional.empty());

    var result = service.computeResultats(promotionId);

    assertThat(result.get(0).parcoursActuel()).isNull();
  }

  @Test
  void computeResultats_shouldExcludeNonStudentUsers() {
    SemesterEntity s1 = semestre(1);
    UserEntity enseignant =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .reference("TCH-001")
            .lastName("Andria")
            .firstName("Ny")
            .role(UserRole.TEACHER)
            .build();
    StudentGroupAssignmentEntity affectation = assignment(enseignant, null);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s1));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s1.getId()))
        .thenReturn(List.of(affectation));

    var result = service.computeResultats(promotionId);

    assertThat(result).isEmpty();
    verify(diplomeEligibilityService, never()).evaluate(any(), any());
  }

  @Test
  void computeResultats_shouldDeduplicateStudent_whenPresentInMultipleSemesters() {
    SemesterEntity s5 = semestre(5);
    SemesterEntity s6 = semestre(6);
    UserEntity etudiant = student("STD-004", "Voahangy", "Lala");
    StudentGroupAssignmentEntity affectationS5 = assignment(etudiant, null);
    StudentGroupAssignmentEntity affectationS6 = assignment(etudiant, Parcours.TN);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s5, s6));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s5.getId()))
        .thenReturn(List.of(affectationS5));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s6.getId()))
        .thenReturn(List.of(affectationS6));
    when(diplomeEligibilityService.evaluate(etudiant.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("11.00")));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(etudiant.getId()))
        .thenReturn(Optional.of(affectationS6));

    var result = service.computeResultats(promotionId);

    assertThat(result).hasSize(1);
    verify(diplomeEligibilityService).evaluate(etudiant.getId(), promotionId);
  }

  @Test
  void computeResultats_shouldSortByMoyenneCumulee_descending() {
    SemesterEntity s6 = semestre(6);
    UserEntity etudiant1 = student("STD-005", "Faniry", "Tia");
    UserEntity etudiant2 = student("STD-006", "Hery", "Nomena");
    StudentGroupAssignmentEntity affectation1 = assignment(etudiant1, Parcours.EL);
    StudentGroupAssignmentEntity affectation2 = assignment(etudiant2, Parcours.EL);

    when(semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId))
        .thenReturn(List.of(s6));
    when(studentGroupAssignmentRepository.findBySemestre_Id(s6.getId()))
        .thenReturn(List.of(affectation1, affectation2));
    when(diplomeEligibilityService.evaluate(etudiant1.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("10.00")));
    when(diplomeEligibilityService.evaluate(etudiant2.getId(), promotionId))
        .thenReturn(new DiplomeEligibilityService.EligibilityResult(true, new BigDecimal("15.00")));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(
            etudiant1.getId()))
        .thenReturn(Optional.of(affectation1));
    when(studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(
            etudiant2.getId()))
        .thenReturn(Optional.of(affectation2));

    var result = service.computeResultats(promotionId);

    assertThat(result)
        .extracting(ResultatStudent::studentId)
        .containsExactly(etudiant2.getId(), etudiant1.getId());
  }
}
