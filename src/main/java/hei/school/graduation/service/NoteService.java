package hei.school.graduation.service;

import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.NoteHistoryEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.ForbiddenException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteHistoryRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class NoteService {

  private final ExamRepository examRepository;
  private final UserRepository userRepository;
  private final NoteRepository noteRepository;
  private final NoteHistoryRepository noteHistoryRepository;
  private final TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final CourseGroupAssignmentRepository courseGroupAssignmentRepository;

  @Transactional
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

    UserEntity caller =
            userRepository
                    .findById(teacherId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + teacherId));

    boolean isAdmin = caller.getRole() == UserRole.ADMIN;
    boolean isAssignedTeacher =
            caller.getRole() == UserRole.TEACHER
                    && teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(
                    teacherId, courseId);

    if (!isAdmin && !isAssignedTeacher) {
      throw new ForbiddenException("User " + teacherId + " is not assigned to course " + courseId);
    }

    StudentGroupAssignmentEntity studentGroupAssignment =
            studentGroupAssignmentRepository
                    .findByStudent_IdAndSemestre_Id(studentId, semestreId)
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
            NoteEntity.builder().exam(exam).student(student).enteredBy(caller).value(valeur).build();

    return noteRepository.save(note);
  }

  @Transactional(readOnly = true)
  public List<NoteEntity> findByExam(UUID examId) {
    if (!examRepository.existsById(examId)) {
      throw new NotFoundException("Exam not found: " + examId);
    }
    return noteRepository.findByExam_Id(examId);
  }

  @Transactional
  public NoteEntity update(UUID noteId, BigDecimal newValue, String reason, UUID modifierId) {
    NoteEntity note =
            noteRepository
                    .findById(noteId)
                    .orElseThrow(() -> new NotFoundException("Note not found: " + noteId));

    UUID courseId = note.getExam().getCourse().getId();

    UserEntity modifier =
            userRepository
                    .findById(modifierId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + modifierId));

    boolean isAdmin = modifier.getRole() == UserRole.ADMIN;
    boolean isAssignedTeacher =
            modifier.getRole() == UserRole.TEACHER
                    && teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(
                    modifierId, courseId);

    if (!isAdmin && !isAssignedTeacher) {
      throw new ForbiddenException(
              "User " + modifierId + " cannot modify notes for course " + courseId);
    }

    NoteHistoryEntity history =
            NoteHistoryEntity.builder()
                    .note(note)
                    .previousValue(note.getValue())
                    .modifiedBy(modifier)
                    .reason(reason)
                    .build();
    noteHistoryRepository.save(history);

    note.setValue(newValue);
    return noteRepository.save(note);
  }

  @Transactional(readOnly = true)
  public List<NoteHistoryEntity> findHistory(UUID noteId) {
    if (!noteRepository.existsById(noteId)) {
      throw new NotFoundException("Note not found: " + noteId);
    }
    return noteHistoryRepository.findByNote_IdOrderByModificationDateDesc(noteId);
  }
}