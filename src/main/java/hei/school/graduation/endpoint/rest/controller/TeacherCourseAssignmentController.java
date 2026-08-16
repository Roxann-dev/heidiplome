package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.TeacherCourseAssignmentCreateRequest;
import hei.school.graduation.mapper.TeacherCourseAssignmentMapper;
import hei.school.graduation.model.TeacherCourseAssignment;
import hei.school.graduation.service.TeacherCourseAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher-course-assignments")
@RequiredArgsConstructor
public class TeacherCourseAssignmentController {

  private final TeacherCourseAssignmentService teacherCourseAssignmentService;
  private final TeacherCourseAssignmentMapper teacherCourseAssignmentMapper;

  @GetMapping
  public List<TeacherCourseAssignment> list(
      @RequestParam(required = false) UUID teacherId,
      @RequestParam(required = false) Integer anneeAcademique) {
    return teacherCourseAssignmentService.findAll(teacherId, anneeAcademique);
  }

  @PostMapping
  public ResponseEntity<TeacherCourseAssignment> create(
      @Valid @RequestBody TeacherCourseAssignmentCreateRequest request) {
    var createdEntity =
        teacherCourseAssignmentService.assign(
            request.teacherId(), request.courseId(), request.anneeAcademique());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(teacherCourseAssignmentMapper.toDomain(createdEntity));
  }
}
