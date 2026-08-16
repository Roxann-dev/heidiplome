package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.GroupCreateRequest;
import hei.school.graduation.model.AcademicGroup;
import hei.school.graduation.model.StudentGroupAssignment;
import hei.school.graduation.service.AcademicGroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AcademicGroupController {

  private final AcademicGroupService academicGroupService;

  @GetMapping("/semestres/{semestreId}/groups")
  public List<AcademicGroup> listBySemester(@PathVariable UUID semestreId) {
    return academicGroupService.findBySemester(semestreId);
  }

  @PostMapping("/semestres/{semestreId}/groups")
  public ResponseEntity<AcademicGroup> create(
      @PathVariable UUID semestreId, @Valid @RequestBody GroupCreateRequest request) {
    var created = academicGroupService.create(semestreId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/groups/{groupId}/students")
  public List<StudentGroupAssignment> students(@PathVariable UUID groupId) {
    return academicGroupService.findStudents(groupId);
  }
}
