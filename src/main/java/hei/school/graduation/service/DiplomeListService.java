package hei.school.graduation.service;

import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.DiplomeEntry;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiplomeListService {

  private static final int DERNIER_SEMESTRE = 6;

  private final SemesterRepository semesterRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final DiplomeEligibilityService diplomeEligibilityService;

  @Transactional(readOnly = true)
  public List<DiplomeEntry> computeDiplomes(UUID promotionId, Parcours parcours) {
    SemesterEntity dernierSemestre =
        semesterRepository
            .findByPromotion_IdAndNumber(promotionId, DERNIER_SEMESTRE)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No semester S" + DERNIER_SEMESTRE + " for this promotion"));

    List<StudentGroupAssignmentEntity> assignmentsS6 =
        studentGroupAssignmentRepository.findBySemestre_IdAndGroup_Parcours(
            dernierSemestre.getId(), parcours);

    List<DiplomeEntry> tries =
        assignmentsS6.stream()
            .map(StudentGroupAssignmentEntity::getStudent)
            .map(student -> toEntryIfDiplome(student, promotionId))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(DiplomeEntry::moyenneGenerale).reversed())
            .toList();

    return IntStream.range(0, tries.size()).mapToObj(i -> withRang(tries.get(i), i + 1)).toList();
  }

  private DiplomeEntry toEntryIfDiplome(UserEntity student, UUID promotionId) {
    var result = diplomeEligibilityService.evaluate(student.getId(), promotionId);

    if (!result.diplome()) {
      return null;
    }

    return new DiplomeEntry(
        0,
        student.getReference(),
        student.getLastName(),
        student.getFirstName(),
        result.moyenneCumulee());
  }

  private DiplomeEntry withRang(DiplomeEntry entry, int rang) {
    return new DiplomeEntry(
        rang, entry.std(), entry.nom(), entry.prenom(), entry.moyenneGenerale());
  }
}
