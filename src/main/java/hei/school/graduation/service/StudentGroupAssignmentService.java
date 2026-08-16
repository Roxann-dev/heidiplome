package hei.school.graduation.service;

import hei.school.graduation.dto.StudentGroupAssignmentCreateRequest;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.StudentGroupAssignmentMapper;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentGroupAssignmentService {

  private final UserRepository userRepository;
  private final AcademicGroupRepository academicGroupRepository;
  private final SemesterRepository semesterRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final StudentGroupAssignmentMapper mapper;

  public List<StudentGroupAssignment> findHistory(UUID studentId) {
    ensureStudentExists(studentId);
    return studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(studentId).stream()
            .map(mapper::toDomain)
            .toList();
  }

  public StudentGroupAssignment assign(UUID studentId, StudentGroupAssignmentCreateRequest request) {
    ensureStudentExists(studentId);

    if (!academicGroupRepository.existsById(request.groupId())) {
      throw new NotFoundException("Group introuvable : " + request.groupId());
    }
    if (!semesterRepository.existsById(request.semestreId())) {
      throw new NotFoundException("Semestre introuvable : " + request.semestreId());
    }
    if (studentGroupAssignmentRepository.existsByStudent_IdAndSemestre_Id(
            studentId, request.semestreId())) {
      throw new ConflictException(
              "Student " + studentId + " a déjà un group pour ce semestre");
    }

    StudentGroupAssignment domain =
            new StudentGroupAssignment(
                    null, studentId, request.groupId(), request.semestreId(),
                    request.dateDebut(), request.dateFin());

    StudentGroupAssignmentEntity entity = mapper.toEntity(domain);
    StudentGroupAssignmentEntity saved = studentGroupAssignmentRepository.save(entity);

    return mapper.toDomain(saved);
  }

  private void ensureStudentExists(UUID studentId) {
    UserEntity student =
            userRepository
                    .findById(studentId)
                    .orElseThrow(() -> new NotFoundException("Student introuvable : " + studentId));

    if (student.getRole() != UserRole.STUDENT) {
      throw new BadRequestException("User " + studentId + " n'a pas le rôle STUDENT");
    }
  }
}