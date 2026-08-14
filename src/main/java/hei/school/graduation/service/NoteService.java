package hei.school.graduation.service;

import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.ForbiddenException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class NoteService {

  private final ExamRepository examRepository;
  private final UserRepository userRepository;
  private final NoteRepository noteRepository;
  private final TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final CourseGroupAssignmentRepository courseGroupAssignmentRepository;

  public NoteEntity saisir(UUID examenId, UUID studentId, BigDecimal valeur, UUID teacherId) {
    ExamEntity exam =
        examRepository
            .findById(examenId)
            .orElseThrow(() -> new NotFoundException("Examen not found: " + examenId));

    UUID courseId = exam.getCourse().getId();
    UUID semestreId = exam.getCourse().getSemester().getId();

    UserEntity student =
        userRepository
            .findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

    if (student.getRole() != UserRole.STUDENT) {
      throw new BadRequestException("User " + studentId + " does not have role STUDENT");
    }

    boolean teacherOwnsCourse =
        teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId);
    if (!teacherOwnsCourse) {
      throw new ForbiddenException(
          "Teacher " + teacherId + " is not assigned to course " + courseId);
    }

    StudentGroupAssignmentEntity studentGroupAssignment =
        studentGroupAssignmentRepository
            .findByStudentIdAndSemestreId(studentId, semestreId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Student "
                            + studentId
                            + " is not assigned to any group for semestre "
                            + semestreId));

    boolean courseFollowedByGroup =
        courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(
            courseId, studentGroupAssignment.getGroup().getId());
    if (!courseFollowedByGroup) {
      throw new BadRequestException(
          "Student "
              + studentId
              + " group does not follow course "
              + courseId
              + " for semestre "
              + semestreId);
    }

    boolean noteAlreadyExists = noteRepository.existsByExam_IdAndStudent_Id(examenId, studentId);
    if (noteAlreadyExists) {
      throw new BadRequestException(
          "Note already exists for exam " + examenId + " and student " + studentId);
    }

    NoteEntity note =
        NoteEntity.builder()
            .exam(exam)
            .student(student)
            .enteredBy(UserEntity.builder().id(teacherId).build())
            .value(valeur)
            .build();

    return noteRepository.save(note);
  }
}
