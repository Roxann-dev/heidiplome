package hei.school.graduation.service;

import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.TeacherCourseAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.BadRequestException;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.mapper.TeacherCourseAssignmentMapper;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.TeacherCourseAssignment;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.TeacherCourseAssignmentRepository;
import hei.school.graduation.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TeacherCourseAssignmentService {

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  private final TeacherCourseAssignmentMapper teacherCourseAssignmentMapper;

  public TeacherCourseAssignmentEntity assign(UUID teacherId, UUID courseId, int anneeAcademique) {
    UserEntity teacher =
        userRepository
            .findById(teacherId)
            .orElseThrow(() -> new NotFoundException("Teacher not found: " + teacherId));

    if (teacher.getRole() != UserRole.TEACHER) {
      throw new BadRequestException("User " + teacherId + " does not have role TEACHER");
    }

    CourseEntity course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));

    boolean alreadyAssigned =
        teacherCourseAssignmentRepository.existsByTeacher_IdAndCourse_IdAndAnneeAcademique(
            teacherId, courseId, anneeAcademique);
    if (alreadyAssigned) {
      throw new ConflictException(
          "Teacher "
              + teacherId
              + " is already assigned to course "
              + courseId
              + " for year "
              + anneeAcademique);
    }

    TeacherCourseAssignmentEntity assignment =
        TeacherCourseAssignmentEntity.builder()
            .teacher(teacher)
            .course(course)
            .anneeAcademique(anneeAcademique)
            .build();

    return teacherCourseAssignmentRepository.save(assignment);
  }

  public List<TeacherCourseAssignment> findAll(UUID teacherId, Integer anneeAcademique) {
    List<TeacherCourseAssignmentEntity> entities;
    if (teacherId != null && anneeAcademique != null) {
      entities =
          teacherCourseAssignmentRepository.findByTeacher_IdAndAnneeAcademique(
              teacherId, anneeAcademique);
    } else if (teacherId != null) {
      entities = teacherCourseAssignmentRepository.findByTeacher_Id(teacherId);
    } else {
      entities = teacherCourseAssignmentRepository.findAll();
    }
    return entities.stream().map(teacherCourseAssignmentMapper::toDomain).toList();
  }
}
