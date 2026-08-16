package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.StudentGroupAssignmentCreateRequest;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.service.StudentGroupAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class StudentGroupAssignmentController {

  private final StudentGroupAssignmentService studentGroupAssignmentService;

  @GetMapping("/{userId}/group-assignments")
  public List<StudentGroupAssignment> history(@PathVariable UUID userId) {
    return studentGroupAssignmentService.findHistory(userId);
  }

  @PostMapping("/{userId}/group-assignments")
  public ResponseEntity<StudentGroupAssignment> assign(
      @PathVariable UUID userId, @Valid @RequestBody StudentGroupAssignmentCreateRequest request) {
    var result = studentGroupAssignmentService.assign(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }
}
