package hei.school.graduation.service;

import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.StatutDiplome;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.ResultatStudent;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ResultatsService {

  private static final int DERNIER_SEMESTRE = 6;

  private final SemesterRepository semesterRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final DiplomeEligibilityService diplomeEligibilityService;

  public List<ResultatStudent> computeResultats(UUID promotionId) {
    List<SemesterEntity> semestres =
            semesterRepository.findByPromotion_IdOrderByNumberAsc(promotionId);

    if (semestres.isEmpty()) {
      throw new NotFoundException("Aucun semestre pour la promotion " + promotionId);
    }

    boolean cursusTermine = semestres.stream().anyMatch(s -> s.getNumber() == DERNIER_SEMESTRE);

    Map<UUID, UserEntity> studentsParId =
            semestres.stream()
                    .flatMap(s -> studentGroupAssignmentRepository.findBySemestre_Id(s.getId()).stream())
                    .map(StudentGroupAssignmentEntity::getStudent)
                    .filter(s -> s.getRole() == UserRole.STUDENT)
                    .collect(Collectors.toMap(UserEntity::getId, s -> s, (a, b) -> a));

    return studentsParId.values().stream()
            .map(student -> toResultat(student, promotionId, cursusTermine))
            .sorted(Comparator.comparing(ResultatStudent::moyenneCumulee).reversed())
            .toList();
  }

  private ResultatStudent toResultat(UserEntity student, UUID promotionId, boolean cursusTermine) {
    var eligibility = diplomeEligibilityService.evaluate(student.getId(), promotionId);

    StatutDiplome statut;
    if (!cursusTermine) {
      statut = StatutDiplome.EN_COURS;
    } else {
      statut = eligibility.diplome() ? StatutDiplome.DIPLOME : StatutDiplome.NON_DIPLOME;
    }

    var currentAssignment =
            studentGroupAssignmentRepository.findTopByStudent_IdOrderByDateDebutDesc(student.getId());

    return new ResultatStudent(
            student.getId(),
            student.getReference(),
            student.getLastName(),
            student.getFirstName(),
            currentAssignment.map(a -> a.getGroup().getParcours()).orElse(null),
            eligibility.moyenneCumulee(),
            statut);
  }
}