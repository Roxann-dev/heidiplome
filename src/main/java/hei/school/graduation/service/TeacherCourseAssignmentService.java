package hei.school.graduation.service;

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
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TeacherCourseAssignmentService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TeacherCourseAssignmentRepository teacherCourseAssignmentRepository;

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
                teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAnneeAcademique(
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
                        .teacherId(teacher.getId())
                        .courseId(course.getId())
                        .anneeAcademique(anneeAcademique)
                        .build();

        return teacherCourseAssignmentRepository.save(assignment);
    }
}