package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.exception.ForbiddenException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.GroupIsolationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupIsolationServiceTest {

  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private CourseGroupAssignmentRepository courseGroupAssignmentRepository;

  private GroupIsolationService service;

  private UUID studentId;
  private UUID semestreId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    service =
        new GroupIsolationService(
            studentGroupAssignmentRepository, courseGroupAssignmentRepository);
    studentId = UUID.randomUUID();
    semestreId = UUID.randomUUID();
    groupId = UUID.randomUUID();
  }

  @Test
  void resolveGroupId_shouldReturnGroupId_whenAssignmentExists() {
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    StudentGroupAssignmentEntity assignment =
        StudentGroupAssignmentEntity.builder().group(group).build();

    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(assignment));

    UUID result = service.resolveGroupId(studentId, semestreId);

    assertThat(result).isEqualTo(groupId);
  }

  @Test
  void resolveGroupId_shouldThrowNotFound_whenNoAssignmentForSemestre() {
    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolveGroupId(studentId, semestreId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(studentId.toString());
  }

  @Test
  void resolveFollowedCourseIds_shouldReturnCourseIds_whenGroupFollowsCourses() {
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    StudentGroupAssignmentEntity assignment =
        StudentGroupAssignmentEntity.builder().group(group).build();

    UUID courseId1 = UUID.randomUUID();
    UUID courseId2 = UUID.randomUUID();

    CourseGroupAssignmentEntity cga1 =
        CourseGroupAssignmentEntity.builder()
            .course(CourseEntity.builder().id(courseId1).build())
            .build();
    CourseGroupAssignmentEntity cga2 =
        CourseGroupAssignmentEntity.builder()
            .course(CourseEntity.builder().id(courseId2).build())
            .build();

    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(assignment));
    when(courseGroupAssignmentRepository.findByGroup_IdAndSemestre_Id(groupId, semestreId))
        .thenReturn(List.of(cga1, cga2));

    List<UUID> result = service.resolveFollowedCourseIds(studentId, semestreId);

    assertThat(result).containsExactlyInAnyOrder(courseId1, courseId2);
  }

  @Test
  void resolveFollowedCourseIds_shouldReturnEmptyList_whenGroupFollowsNoCourse() {
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    StudentGroupAssignmentEntity assignment =
        StudentGroupAssignmentEntity.builder().group(group).build();

    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(assignment));
    when(courseGroupAssignmentRepository.findByGroup_IdAndSemestre_Id(groupId, semestreId))
        .thenReturn(List.of());

    List<UUID> result = service.resolveFollowedCourseIds(studentId, semestreId);

    assertThat(result).isEmpty();
  }

  @Test
  void checkStudentFollowsCourse_shouldNotThrow_whenStudentFollowsCourse() {
    UUID courseId = UUID.randomUUID();
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    StudentGroupAssignmentEntity assignment =
        StudentGroupAssignmentEntity.builder().group(group).build();
    CourseGroupAssignmentEntity cga =
        CourseGroupAssignmentEntity.builder()
            .course(CourseEntity.builder().id(courseId).build())
            .build();

    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(assignment));
    when(courseGroupAssignmentRepository.findByGroup_IdAndSemestre_Id(groupId, semestreId))
        .thenReturn(List.of(cga));

    assertThatCode(() -> service.checkStudentFollowsCourse(studentId, courseId, semestreId))
        .doesNotThrowAnyException();
  }

  @Test
  void checkStudentFollowsCourse_shouldThrowForbidden_whenStudentDoesNotFollowCourse() {
    UUID followedCourseId = UUID.randomUUID();
    UUID otherCourseId = UUID.randomUUID();
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    StudentGroupAssignmentEntity assignment =
        StudentGroupAssignmentEntity.builder().group(group).build();
    CourseGroupAssignmentEntity cga =
        CourseGroupAssignmentEntity.builder()
            .course(CourseEntity.builder().id(followedCourseId).build())
            .build();

    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.of(assignment));
    when(courseGroupAssignmentRepository.findByGroup_IdAndSemestre_Id(groupId, semestreId))
        .thenReturn(List.of(cga));

    assertThatThrownBy(
            () -> service.checkStudentFollowsCourse(studentId, otherCourseId, semestreId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining(otherCourseId.toString());
  }

  @Test
  void checkStudentFollowsCourse_shouldPropagateNotFound_whenStudentHasNoGroupForSemestre() {
    UUID courseId = UUID.randomUUID();

    when(studentGroupAssignmentRepository.findByStudent_IdAndSemestre_Id(studentId, semestreId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.checkStudentFollowsCourse(studentId, courseId, semestreId))
        .isInstanceOf(NotFoundException.class);
  }
}
