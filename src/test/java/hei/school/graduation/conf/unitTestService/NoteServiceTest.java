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
import hei.school.graduation.repository.NoteHistoryRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import hei.school.graduation.service.NoteService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

  @Mock private ExamRepository examRepository;
  @Mock private UserRepository userRepository;
  @Mock private NoteRepository noteRepository;
  @Mock private NoteHistoryRepository noteHistoryRepository;
  @Mock private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private CourseGroupAssignmentRepository courseGroupAssignmentRepository;

  private NoteService service;

  private UUID examenId;
  private UUID studentId;
  private UUID teacherId;
  private UUID adminId;
  private UUID courseId;
  private UUID semestreId;
  private UUID groupId;
  private BigDecimal valeur;

  private ExamEntity exam;
  private UserEntity student;
  private UserEntity teacherUser;
  private UserEntity adminUser;
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
            noteHistoryRepository,
            teacherCourseAssignmentRepository,
            studentGroupAssignmentRepository,
            courseGroupAssignmentRepository);

    examenId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
    adminId = UUID.randomUUID();
    courseId = UUID.randomUUID();
    semestreId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    valeur = new BigDecimal("15.50");

    semester = SemesterEntity.builder().id(semestreId).build();
    CourseEntity course = CourseEntity.builder().id(courseId).semester(semester).build();
    exam = ExamEntity.builder().id(examenId).course(course).build();
    student = UserEntity.builder().id(studentId).role(UserRole.STUDENT).build();
    teacherUser = UserEntity.builder().id(teacherId).role(UserRole.TEACHER).build();
    adminUser = UserEntity.builder().id(adminId).role(UserRole.ADMIN).build();
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
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(studentGroupAssignment));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(true);
    when(noteRepository.existsByExam_IdAndStudent_Id(examenId, studentId)).thenReturn(false);
    when(noteRepository.save(any(NoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteEntity result = service.saisir(examenId, studentId, valeur, teacherId);

    assertThat(result.getExam()).isEqualTo(exam);
    assertThat(result.getStudent()).isEqualTo(student);
    assertThat(result.getEnteredBy()).isEqualTo(teacherUser);
    assertThat(result.getValue()).isEqualByComparingTo(valeur);
    verify(noteRepository).save(any(NoteEntity.class));
  }

  @Test
  void saisir_shouldCreateNote_whenCallerIsAdmin() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(studentGroupAssignment));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(true);
    when(noteRepository.existsByExam_IdAndStudent_Id(examenId, studentId)).thenReturn(false);
    when(noteRepository.save(any(NoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteEntity result = service.saisir(examenId, studentId, valeur, adminId);

    assertThat(result.getEnteredBy()).isEqualTo(adminUser);
    verify(teacherCourseAssignmentRepository, never()).existsByTeacher_IdAndCourse_Id(any(), any());
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
  void saisir_shouldThrowNotFound_whenCallerDoesNotExist() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(teacherId.toString());

    verifyNoInteractions(teacherCourseAssignmentRepository, noteRepository);
  }

  @Test
  void saisir_shouldThrowForbidden_whenTeacherNotAssignedToCourse() {
    when(examRepository.findById(examenId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
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
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
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
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
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
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(studentGroupAssignment));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(true);
    when(noteRepository.existsByExam_IdAndStudent_Id(examenId, studentId)).thenReturn(true);

    assertThatThrownBy(() -> service.saisir(examenId, studentId, valeur, teacherId))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already exists");

    verify(noteRepository, never()).save(any());
  }

  @Test
  void findByExam_shouldReturnNotes_whenExamExists() {
    NoteEntity note = NoteEntity.builder().id(UUID.randomUUID()).exam(exam).build();
    when(examRepository.existsById(examenId)).thenReturn(true);
    when(noteRepository.findByExam_Id(examenId)).thenReturn(List.of(note));

    List<NoteEntity> result = service.findByExam(examenId);

    assertThat(result).containsExactly(note);
  }

  @Test
  void findByExam_shouldThrowNotFound_whenExamDoesNotExist() {
    when(examRepository.existsById(examenId)).thenReturn(false);

    assertThatThrownBy(() -> service.findByExam(examenId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(examenId.toString());

    verifyNoInteractions(noteRepository);
  }

  @Test
  void update_shouldArchiveOldValueAndUpdateNote_whenAdminModifies() {
    UUID noteId = UUID.randomUUID();
    BigDecimal oldValue = new BigDecimal("8.00");
    BigDecimal newValue = new BigDecimal("12.00");
    NoteEntity note =
        NoteEntity.builder().id(noteId).exam(exam).student(student).value(oldValue).build();

    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
    when(noteRepository.save(any(NoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteEntity result = service.update(noteId, newValue, "Erreur de saisie corrigée", adminId);

    ArgumentCaptor<NoteHistoryEntity> historyCaptor =
        ArgumentCaptor.forClass(NoteHistoryEntity.class);
    verify(noteHistoryRepository).save(historyCaptor.capture());

    assertThat(historyCaptor.getValue().getPreviousValue()).isEqualByComparingTo(oldValue);
    assertThat(historyCaptor.getValue().getModifiedBy()).isEqualTo(adminUser);
    assertThat(historyCaptor.getValue().getReason()).isEqualTo("Erreur de saisie corrigée");
    assertThat(result.getValue()).isEqualByComparingTo(newValue);
  }

  @Test
  void update_shouldSucceed_whenAssignedTeacherModifies() {
    UUID noteId = UUID.randomUUID();
    NoteEntity note =
        NoteEntity.builder()
            .id(noteId)
            .exam(exam)
            .student(student)
            .value(new BigDecimal("8.00"))
            .build();

    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(true);
    when(noteRepository.save(any(NoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteEntity result = service.update(noteId, new BigDecimal("11.00"), "Réclamation", teacherId);

    assertThat(result.getValue()).isEqualByComparingTo("11.00");
    verify(noteHistoryRepository).save(any(NoteHistoryEntity.class));
  }

  @Test
  void update_shouldThrowForbidden_whenTeacherNotAssignedToCourse() {
    UUID noteId = UUID.randomUUID();
    NoteEntity note =
        NoteEntity.builder()
            .id(noteId)
            .exam(exam)
            .student(student)
            .value(new BigDecimal("8.00"))
            .build();

    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacherUser));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_Id(teacherId, courseId))
        .thenReturn(false);

    assertThatThrownBy(
            () -> service.update(noteId, new BigDecimal("11.00"), "Réclamation", teacherId))
        .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(noteHistoryRepository);
    verify(noteRepository, never()).save(any());
  }

  @Test
  void update_shouldThrowNotFound_whenNoteDoesNotExist() {
    UUID noteId = UUID.randomUUID();
    when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(noteId, new BigDecimal("11.00"), "Réclamation", adminId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(noteId.toString());

    verifyNoInteractions(userRepository, noteHistoryRepository);
  }

  @Test
  void update_shouldThrowNotFound_whenModifierDoesNotExist() {
    UUID noteId = UUID.randomUUID();
    NoteEntity note =
        NoteEntity.builder()
            .id(noteId)
            .exam(exam)
            .student(student)
            .value(new BigDecimal("8.00"))
            .build();

    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(userRepository.findById(adminId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(noteId, new BigDecimal("11.00"), "Réclamation", adminId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(adminId.toString());

    verifyNoInteractions(noteHistoryRepository);
  }

  @Test
  void findHistory_shouldReturnHistory_whenNoteExists() {
    UUID noteId = UUID.randomUUID();
    NoteHistoryEntity historyEntry = NoteHistoryEntity.builder().id(UUID.randomUUID()).build();

    when(noteRepository.existsById(noteId)).thenReturn(true);
    when(noteHistoryRepository.findByNote_IdOrderByModificationDateDesc(noteId))
        .thenReturn(List.of(historyEntry));

    List<NoteHistoryEntity> result = service.findHistory(noteId);

    assertThat(result).containsExactly(historyEntry);
  }

  @Test
  void findHistory_shouldThrowNotFound_whenNoteDoesNotExist() {
    UUID noteId = UUID.randomUUID();
    when(noteRepository.existsById(noteId)).thenReturn(false);

    assertThatThrownBy(() -> service.findHistory(noteId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(noteId.toString());

    verifyNoInteractions(noteHistoryRepository);
  }
}
