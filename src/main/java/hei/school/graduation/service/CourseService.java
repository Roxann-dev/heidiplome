package hei.school.graduation.service;

import hei.school.graduation.dto.CourseCreateRequest;
import hei.school.graduation.dto.CourseGroupAssignmentCreateRequest;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.CourseGroupAssignmentEntity;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;
  private final SemesterRepository semesterRepository;
  private final AcademicGroupRepository academicGroupRepository;
  private final CourseGroupAssignmentRepository courseGroupAssignmentRepository;
  private final CourseMapper courseMapper;
  private final CourseGroupAssignmentMapper courseGroupAssignmentMapper;

  public List<Course> findAll(UUID semestreId) {
    var entities =
        semestreId == null
            ? courseRepository.findAll()
            : courseRepository.findBySemester_Id(semestreId);
    return entities.stream().map(courseMapper::toDomain).toList();
  }

  public Course findById(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .map(courseMapper::toDomain)
        .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
  }

  public Course create(CourseCreateRequest request) {
    var semester =
        semesterRepository
            .findById(request.semestreId())
            .orElseThrow(
                () -> new NotFoundException("Semester not found: " + request.semestreId()));

    CourseEntity entity =
        CourseEntity.builder()
            .referenceCs(request.referenceCs())
            .title(request.title())
            .credits(request.credits())
            .semester(semester)
            .build();

    return courseMapper.toDomain(courseRepository.save(entity));
  }

  public CourseGroupAssignment assignGroup(
      UUID courseId, CourseGroupAssignmentCreateRequest request) {
    var course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

    var group =
        academicGroupRepository
            .findById(request.groupId())
            .orElseThrow(() -> new NotFoundException("Group not found: " + request.groupId()));

    var semester =
        semesterRepository
            .findById(request.semestreId())
            .orElseThrow(
                () -> new NotFoundException("Semester not found: " + request.semestreId()));

    if (courseGroupAssignmentRepository.existsByCourse_IdAndGroup_Id(courseId, request.groupId())) {
      throw new ConflictException(
          "Course " + courseId + " is already associated with group " + request.groupId());
    }

    CourseGroupAssignmentEntity entity =
        CourseGroupAssignmentEntity.builder()
            .course(course)
            .group(group)
            .semestre(semester)
            .build();

    return courseGroupAssignmentMapper.toDomain(courseGroupAssignmentRepository.save(entity));
  }
}
