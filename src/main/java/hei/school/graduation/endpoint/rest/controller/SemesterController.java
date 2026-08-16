package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.model.Semester;
import hei.school.graduation.service.SemesterService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SemesterController {

  private final SemesterService semesterService;

  @GetMapping("/promotions/{promotionId}/semestres")
  public List<Semester> listByPromotion(@PathVariable UUID promotionId) {
    return semesterService.findByPromotion(promotionId);
  }
}
