package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.CourseCreateRequest;
import hei.school.graduation.dto.CourseGroupAssignmentCreateRequest;
import hei.school.graduation.model.Course;
import hei.school.graduation.model.CourseGroupAssignment;
import hei.school.graduation.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

  private final CourseService courseService;

  @GetMapping
  public List<Course> list(@RequestParam(required = false) UUID semestreId) {
    return courseService.findAll(semestreId);
  }

  @GetMapping("/{courseId}")
  public Course get(@PathVariable UUID courseId) {
    return courseService.findById(courseId);
  }

  @PostMapping
  public ResponseEntity<Course> create(@Valid @RequestBody CourseCreateRequest request) {
    var created = courseService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PostMapping("/{courseId}/groups")
  public ResponseEntity<CourseGroupAssignment> assignGroup(
      @PathVariable UUID courseId, @Valid @RequestBody CourseGroupAssignmentCreateRequest request) {
    var created = courseService.assignGroup(courseId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
