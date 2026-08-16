package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.ExamCreateRequest;
import hei.school.graduation.mapper.ExamMapper;
import hei.school.graduation.model.Exam;
import hei.school.graduation.service.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamController {

  private final ExamService examService;
  private final ExamMapper examMapper;

  @GetMapping("/courses/{courseId}/examens")
  public List<Exam> listByCourse(@PathVariable UUID courseId) {
    return examService.findByCourse(courseId).stream().map(examMapper::toDomain).toList();
  }

  @PostMapping("/courses/{courseId}/examens")
  public ResponseEntity<Exam> create(
      @PathVariable UUID courseId, @Valid @RequestBody ExamCreateRequest request) {
    var created =
        examService.create(courseId, request.examDate(), request.coefficient(), request.type());
    return ResponseEntity.status(HttpStatus.CREATED).body(examMapper.toDomain(created));
  }

  @GetMapping("/examens/{examenId}")
  public Exam getById(@PathVariable UUID examenId) {
    return examMapper.toDomain(examService.findById(examenId));
  }
}
