package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.service.StudentGroupAssignmentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class StudentGroupAssignmentController {

  private final StudentGroupAssignmentService studentGroupAssignmentService;

  @GetMapping("/{userId}/group-assignments")
  public List<StudentGroupAssignment> history(@PathVariable UUID userId) {
    return studentGroupAssignmentService.findHistory(userId);
  }
}
