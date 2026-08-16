package hei.school.graduation.service;

import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.BadRequestException;
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
