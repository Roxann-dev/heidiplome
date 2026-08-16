package hei.school.graduation.service;

import hei.school.graduation.dto.GroupCreateRequest;
import hei.school.graduation.entity.AcademicGroupEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.AcademicGroupMapper;
import hei.school.graduation.mapper.StudentGroupAssignmentMapper;
import hei.school.graduation.model.AcademicGroup;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.repository.AcademicGroupRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicGroupService {

  private final AcademicGroupRepository academicGroupRepository;
  private final SemesterRepository semesterRepository;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final AcademicGroupMapper academicGroupMapper;
  private final StudentGroupAssignmentMapper studentGroupAssignmentMapper;

  public List<AcademicGroup> findBySemester(UUID semestreId) {
    if (!semesterRepository.existsById(semestreId)) {
      throw new NotFoundException("Semester not found: " + semestreId);
    }
    return academicGroupRepository.findBySemester_Id(semestreId).stream()
        .map(academicGroupMapper::toDomain)
        .toList();
  }

  public AcademicGroup create(UUID semestreId, GroupCreateRequest request) {
    var semester =
        semesterRepository
            .findById(semestreId)
            .orElseThrow(() -> new NotFoundException("Semester not found: " + semestreId));

    AcademicGroupEntity entity =
        AcademicGroupEntity.builder()
            .reference(request.reference())
            .parcours(request.parcours())
            .semester(semester)
            .build();

    return academicGroupMapper.toDomain(academicGroupRepository.save(entity));
  }

  public List<StudentGroupAssignment> findStudents(UUID groupId) {
    if (!academicGroupRepository.existsById(groupId)) {
      throw new NotFoundException("Group not found: " + groupId);
    }
    return studentGroupAssignmentRepository.findByGroup_Id(groupId).stream()
        .map(studentGroupAssignmentMapper::toDomain)
        .toList();
  }
}
