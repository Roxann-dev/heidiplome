package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.model.ResultatStudent;
import hei.school.graduation.service.ResultatsService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class ResultatsController {

  private final ResultatsService resultatsService;

  @GetMapping("/{promotionId}/resultats")
  public List<ResultatStudent> resultats(@PathVariable UUID promotionId) {
    return resultatsService.computeResultats(promotionId);
  }
}
