package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import hei.school.graduation.dto.CourseCreateRequest;
import hei.school.graduation.dto.CourseGroupAssignmentCreateRequest;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.CourseGroupAssignmentMapper;
import hei.school.graduation.mapper.CourseMapper;
import hei.school.graduation.model.Course;
import hei.school.graduation.model.CourseGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.CourseGroupAssignmentRepository;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.service.CourseService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock private CourseRepository courseRepository;
  @Mock private SemesterRepository semesterRepository;
  @Mock private AcademicGroupRepository academicGroupRepository;
  @Mock private CourseGroupAssignmentRepository courseGroupAssignmentRepository;
  @Mock private CourseMapper courseMapper;
  @Mock private CourseGroupAssignmentMapper courseGroupAssignmentMapper;

  private CourseService service;

  private UUID courseId;
  private UUID semesterId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    service =
        new CourseService(
            courseRepository,
            semesterRepository,
            academicGroupRepository,
            courseGroupAssignmentRepository,
            courseMapper,
            courseGroupAssignmentMapper);
    courseId = UUID.randomUUID();
    semesterId = UUID.randomUUID();
    groupId = UUID.randomUUID();
  }

  @Test
  void findAll_shouldReturnAllCourses_whenSemestreIdIsNull() {
    CourseEntity entity = CourseEntity.builder().id(courseId).build();
    Course domain = new Course(courseId, "PROG4", "Prog avancée", 5, semesterId);

    when(courseRepository.findAll()).thenReturn(List.of(entity));
    when(courseMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findAll(null);

    assertThat(result).containsExactly(domain);
  }

  @Test
  void findAll_shouldReturnFilteredCourses_whenSemestreIdProvided() {
    CourseEntity entity = CourseEntity.builder().id(courseId).build();
    Course domain = new Course(courseId, "PROG4", "Prog avancée", 5, semesterId);

    when(courseRepository.findBySemester_Id(semesterId)).thenReturn(List.of(entity));
    when(courseMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findAll(semesterId);

    assertThat(result).containsExactly(domain);
  }

  @Test
  void findById_shouldReturnMappedCourse_whenExists() {
    CourseEntity entity = CourseEntity.builder().id(courseId).build();
    Course domain = new Course(courseId, "PROG4", "Prog avancée", 5, semesterId);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(entity));
    when(courseMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findById(courseId);

    assertThat(result).isEqualTo(domain);
  }

  @Test
  void findById_shouldThrowNotFound_whenCourseDoesNotExist() {
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(courseId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(courseId.toString());
  }

  @Test
  void create_shouldSaveCourse_whenSemesterExists() {
    SemesterEntity semester = SemesterEntity.builder().id(semesterId).build();
    CourseCreateRequest request = new CourseCreateRequest("PROG4", "Prog avancée", 5, semesterId);
    CourseEntity savedEntity =
        CourseEntity.builder()
            .id(courseId)
            .referenceCs("PROG4")
            .title("Prog avancée")
            .credits(5)
            .semester(semester)
            .build();
    Course domain = new Course(courseId, "PROG4", "Prog avancée", 5, semesterId);

    when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
    when(courseRepository.save(any(CourseEntity.class))).thenReturn(savedEntity);
    when(courseMapper.toDomain(savedEntity)).thenReturn(domain);

    var result = service.create(request);

    assertThat(result).isEqualTo(domain);
  }

  @Test
  void create_shouldThrowNotFound_whenSemesterDoesNotExist() {
    CourseCreateRequest request = new CourseCreateRequest("PROG4", "Prog avancée", 5, semesterId);

    when(semesterRepository.findById(semesterId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(semesterId.toString());
  }

  @Test
  void assignGroup_shouldCreateAssignment_whenAllEntitiesExistAndNotAlreadyAssigned() {
    CourseEntity course = CourseEntity.builder().id(courseId).build();
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    SemesterEntity semester = SemesterEntity.builder().id(semesterId).build();
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(groupId, semesterId);
    CourseGroupAssignmentEntity savedEntity =
        CourseGroupAssignmentEntity.builder()
            .course(course)
            .group(group)
            .semestre(semester)
            .build();
    CourseGroupAssignment domain =
        new CourseGroupAssignment(UUID.randomUUID(), courseId, groupId, semesterId);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(academicGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(false);
    when(courseGroupAssignmentRepository.save(any(CourseGroupAssignmentEntity.class)))
        .thenReturn(savedEntity);
    when(courseGroupAssignmentMapper.toDomain(savedEntity)).thenReturn(domain);

    var result = service.assignGroup(courseId, request);

    assertThat(result).isEqualTo(domain);
  }

  @Test
  void assignGroup_shouldThrowNotFound_whenCourseDoesNotExist() {
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(groupId, semesterId);

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assignGroup(courseId, request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(courseId.toString());
  }

  @Test
  void assignGroup_shouldThrowNotFound_whenGroupDoesNotExist() {
    CourseEntity course = CourseEntity.builder().id(courseId).build();
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(groupId, semesterId);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(academicGroupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assignGroup(courseId, request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(groupId.toString());
  }

  @Test
  void assignGroup_shouldThrowConflict_whenAlreadyAssigned() {
    CourseEntity course = CourseEntity.builder().id(courseId).build();
    AcademicGroupEntity group = AcademicGroupEntity.builder().id(groupId).build();
    SemesterEntity semester = SemesterEntity.builder().id(semesterId).build();
    CourseGroupAssignmentCreateRequest request =
        new CourseGroupAssignmentCreateRequest(groupId, semesterId);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(academicGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
    when(courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, groupId))
        .thenReturn(true);

    assertThatThrownBy(() -> service.assignGroup(courseId, request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already associated");
  }
}
