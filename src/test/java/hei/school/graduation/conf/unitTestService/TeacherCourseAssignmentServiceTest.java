package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import hei.school.graduation.service.TeacherCourseAssignmentService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeacherCourseAssignmentServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;

  private TeacherCourseAssignmentService service;

  private UUID teacherId;
  private UUID courseId;
  private int anneeAcademique;

  @BeforeEach
  void setUp() {
    service =
        new TeacherCourseAssignmentService(
            userRepository, courseRepository, teacherCourseAssignmentRepository);
    teacherId = UUID.randomUUID();
    courseId = UUID.randomUUID();
    anneeAcademique = 2026;
  }

  @Test
  void assign_shouldCreateAssignment_whenTeacherAndCourseAreValid() {
    UserEntity teacher = UserEntity.builder().id(teacherId).role(UserRole.TEACHER).build();
    CourseEntity course = CourseEntity.builder().id(courseId).build();

    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_IdAndAnneeAcademique(
            teacherId, courseId, anneeAcademique))
        .thenReturn(false);
    when(teacherCourseAssignmentRepository.save(any(TeacherCourseAssignmentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TeacherCourseAssignmentEntity result = service.assign(teacherId, courseId, anneeAcademique);

    assertThat(result.getTeacher()).isEqualTo(teacherId);
    assertThat(result.getCourse()).isEqualTo(courseId);
    assertThat(result.getAnneeAcademique()).isEqualTo(anneeAcademique);
    verify(teacherCourseAssignmentRepository).save(any(TeacherCourseAssignmentEntity.class));
  }

  @Test
  void assign_shouldThrowNotFound_whenTeacherDoesNotExist() {
    when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assign(teacherId, courseId, anneeAcademique))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(teacherId.toString());

    verifyNoInteractions(courseRepository, teacherCourseAssignmentRepository);
  }

  @Test
  void assign_shouldThrowBadRequest_whenUserIsNotTeacher() {
    UserEntity notATeacher = UserEntity.builder().id(teacherId).role(UserRole.STUDENT).build();

    when(userRepository.findById(teacherId)).thenReturn(Optional.of(notATeacher));

    assertThatThrownBy(() -> service.assign(teacherId, courseId, anneeAcademique))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("does not have role TEACHER");

    verifyNoInteractions(courseRepository, teacherCourseAssignmentRepository);
  }

  @Test
  void assign_shouldThrowNotFound_whenCourseDoesNotExist() {
    UserEntity teacher = UserEntity.builder().id(teacherId).role(UserRole.TEACHER).build();

    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assign(teacherId, courseId, anneeAcademique))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(courseId.toString());

    verifyNoInteractions(teacherCourseAssignmentRepository);
  }

  @Test
  void assign_shouldThrowConflict_whenAssignmentAlreadyExists() {
    UserEntity teacher = UserEntity.builder().id(teacherId).role(UserRole.TEACHER).build();
    CourseEntity course = CourseEntity.builder().id(courseId).build();

    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_IdAndAnneeAcademique(
            teacherId, courseId, anneeAcademique))
        .thenReturn(true);

    assertThatThrownBy(() -> service.assign(teacherId, courseId, anneeAcademique))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already assigned");

    verify(teacherCourseAssignmentRepository, never()).save(any());
  }
}
