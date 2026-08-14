package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hei.school.graduation.entity.*;
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
import hei.school.graduation.service.NoteService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

  @Mock private ExamRepository examRepository;
  @Mock private UserRepository userRepository;
  @Mock private NoteRepository noteRepository;
  @Mock private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private CourseGroupAssignmentRepository courseGroupAssignmentRepository;

  private NoteService service;

  private UUID examenId;
  private UUID studentId;
  private UUID teacherId;
  private UUID courseId;
  private UUID semestreId;
  private UUID groupId;
  private BigDecimal valeur;

  private ExamEntity exam;
  private UserEntity student;
  private AcademicGroupEntity group;
  private SemesterEntity semester;
  private StudentGroupAssignmentEntity studentGroupAssignment;

  @BeforeEach
  void setUp() {
    service =
        new NoteService(
            examRepository,
            userRepository,
            noteRepository,
            teacherCourseAssignmentRepository,
            studentGroupAssignmentRepository,
            courseGroupAssignmentRepository);

    examenId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
    courseId = UUID.randomUUID();
    semestreId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    valeur = new BigDecimal("15.50");

    SemesterEntity semester = SemesterEntity.builder().id(semestreId).build();
    CourseEntity course = CourseEntity.builder().id(courseId).semester(semester).build();
    exam = ExamEntity.builder().id(examenId).course(course).build();
    student = UserEntity.builder().id(studentId).role(UserRole.STUDENT).build();
    group = AcademicGroupEntity.builder().id(groupId).build();
    studentGroupAssignment =
        StudentGroupAssignmentEntity.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .semestre(semester)
            .build();
  }

  @Test
  void saisir_shouldCreateNote_whenAllChecksPass() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudentIdAndSemestreId(studentId, semestreId))
        .thenReturn(Optional.of(studentGroupAssignment));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(true);
    when(noteRepository.existsByExam_IdAndStudent_Id(examenId, studentId)).thenReturn(false);
    when(noteRepository.save(any(NoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteEntity result = service.saisir(examenId, studentId, valeur, teacherId);

    assertThat(result.getExam()).isEqualTo(exam);
    assertThat(result.getStudent()).isEqualTo(student);
    assertThat(result.getValue()).isEqualByComparingTo(valeur);
    verify(noteRepository).save(any(NoteEntity.class));
  }

  @Test
  void saisir_shouldThrowNotFound_whenExamDoesNotExist() {
    when(examRepository.findById(examenId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(examenId.toString());

    verifyNoInteractions(userRepository, noteRepository, teacherCourseAssignmentRepository);
  }

  @Test
  void saisir_shouldThrowNotFound_whenStudentDoesNotExist() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(studentId.toString());

    verifyNoInteractions(teacherCourseAssignmentRepository, noteRepository);
  }

  @Test
  void saisir_shouldThrowBadRequest_whenUserIsNotStudent() {
    UserEntity notAStudent = UserEntity.builder().id(studentId).role(UserRole.TEACHER).build();

    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(notAStudent));

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("does not have role STUDENT");

    verifyNoInteractions(teacherCourseAssignmentRepository, noteRepository);
  }

  @Test
  void saisir_shouldThrowForbidden_whenTeacherNotAssignedToCourse() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("not assigned to course");

    verifyNoInteractions(studentGroupAssignmentRepository, noteRepository);
  }

  @Test
  void saisir_shouldThrowBadRequest_whenStudentHasNoGroupForSemestre() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudentIdAndSemestreId(studentId, semestreId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not assigned to any group");

    verifyNoInteractions(courseGroupAssignmentRepository, noteRepository);
  }

  @Test
  void saisir_shouldThrowBadRequest_whenGroupDoesNotFollowCourse() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudentIdAndSemestreId(studentId, semestreId))
        .thenReturn(Optional.of(studentGroupAssignment));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("does not follow course");

    verifyNoInteractions(noteRepository);
  }

  @Test
  void saisir_shouldThrowBadRequest_whenNoteAlreadyExists() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudentIdAndSemestreId(studentId, semestreId))
        .thenReturn(Optional.of(studentGroupAssignment));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(true);
    when(noteRepository.existsByExam_IdAndStudent_Id(examenId, studentId)).thenReturn(true);

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already exists");

    verify(noteRepository, never()).save(any());
  }
}
