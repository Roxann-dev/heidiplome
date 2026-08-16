package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import hei.school.graduation.dto.GroupCreateRequest;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.AcademicGroupMapper;
import hei.school.graduation.mapper.StudentGroupAssignmentMapper;
import hei.school.graduation.model.AcademicGroup;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.AcademicGroupService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicGroupServiceTest {

  @Mock private AcademicGroupRepository academicGroupRepository;
  @Mock private SemesterRepository semesterRepository;
  @Mock private StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  @Mock private AcademicGroupMapper academicGroupMapper;
  @Mock private StudentGroupAssignmentMapper studentGroupAssignmentMapper;

  private AcademicGroupService service;

  private UUID semesterId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    service =
        new AcademicGroupService(
            academicGroupRepository,
            semesterRepository,
            studentGroupAssignmentRepository,
            academicGroupMapper,
            studentGroupAssignmentMapper);
    semesterId = UUID.randomUUID();
    groupId = UUID.randomUUID();
  }

  @Test
  void findBySemester_shouldReturnMappedList_whenSemesterExists() {
    AcademicGroupEntity entity = AcademicGroupEntity.builder().id(groupId).build();
    AcademicGroup domain = new AcademicGroup(groupId, "K1", null, semesterId);

    when(semesterRepository.existsById(semesterId)).thenReturn(true);
    when(academicGroupRepository.findBySemester_Id(semesterId)).thenReturn(List.of(entity));
    when(academicGroupMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findBySemester(semesterId);

    assertThat(result).containsExactly(domain);
  }

  @Test
  void findBySemester_shouldThrowNotFound_whenSemesterDoesNotExist() {
    when(semesterRepository.existsById(semesterId)).thenReturn(false);

    assertThatThrownBy(() -> service.findBySemester(semesterId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(semesterId.toString());
  }

  @Test
  void create_shouldSaveGroup_whenSemesterExists() {
    SemesterEntity semester = SemesterEntity.builder().id(semesterId).build();
    GroupCreateRequest request = new GroupCreateRequest("K1-EL", Parcours.EL);
    AcademicGroupEntity savedEntity =
        AcademicGroupEntity.builder()
            .id(groupId)
            .reference("K1-EL")
            .parcours(Parcours.EL)
            .semester(semester)
            .build();
    AcademicGroup domain = new AcademicGroup(groupId, "K1-EL", Parcours.EL, semesterId);

    when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
    when(academicGroupRepository.save(any(AcademicGroupEntity.class))).thenReturn(savedEntity);
    when(academicGroupMapper.toDomain(savedEntity)).thenReturn(domain);

    var result = service.create(semesterId, request);

    assertThat(result).isEqualTo(domain);
  }

  @Test
  void create_shouldThrowNotFound_whenSemesterDoesNotExist() {
    GroupCreateRequest request = new GroupCreateRequest("K1-EL", Parcours.EL);

    when(semesterRepository.findById(semesterId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(semesterId, request))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(semesterId.toString());
  }

  @Test
  void findStudents_shouldReturnMappedList_whenGroupExists() {
    StudentGroupAssignmentEntity entity =
        StudentGroupAssignmentEntity.builder().dateDebut(LocalDate.now()).build();
    StudentGroupAssignment domain =
        new StudentGroupAssignment(
            UUID.randomUUID(), UUID.randomUUID(), groupId, semesterId, LocalDate.now(), null);

    when(academicGroupRepository.existsById(groupId)).thenReturn(true);
    when(studentGroupAssignmentRepository.findByGroup_Id(groupId)).thenReturn(List.of(entity));
    when(studentGroupAssignmentMapper.toDomain(entity)).thenReturn(domain);

    var result = service.findStudents(groupId);

    assertThat(result).containsExactly(domain);
  }

  @Test
  void findStudents_shouldThrowNotFound_whenGroupDoesNotExist() {
    when(academicGroupRepository.existsById(groupId)).thenReturn(false);

    assertThatThrownBy(() -> service.findStudents(groupId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(groupId.toString());
  }
}
