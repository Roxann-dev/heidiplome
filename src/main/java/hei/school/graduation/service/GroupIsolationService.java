package hei.school.graduation.service;

import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.exception.ForbiddenException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupIsolationService {

  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final CourseGroupAssignmentRepository courseGroupAssignmentRepository;

  public UUID resolveGroupId(UUID studentId, UUID semestreId) {
    StudentGroupAssignmentEntity assignment =
        studentGroupAssignmentRepository
            .findByStudent_IdAndSemestre_Id(studentId, semestreId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Student "
                            + studentId
                            + " n'a pas de group pour le semestre "
                            + semestreId));
    return assignment.getGroup().getId();
  }

  public List<UUID> resolveFollowedCourseIds(UUID studentId, UUID semestreId) {
    UUID groupId = resolveGroupId(studentId, semestreId);
    return courseGroupAssignmentRepository
        .findByGroup_IdAndSemestre_Id(groupId, semestreId)
        .stream()
        .map(cga -> cga.getCourse().getId())
        .collect(Collectors.toList());
  }

  public void checkStudentFollowsCourse(UUID studentId, UUID courseId, UUID semestreId) {
    if (!resolveFollowedCourseIds(studentId, semestreId).contains(courseId)) {
      throw new ForbiddenException(
          "Student " + studentId + " ne suit pas le course " + courseId + " sur ce semestre");
    }
  }
}
