package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.model.Promotion;
import hei.school.graduation.service.PromotionService;
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
public class PromotionController {

  private final PromotionService promotionService;

  @GetMapping
  public List<Promotion> list() {
    return promotionService.findAll();
  }

  @GetMapping("/{promotionId}")
  public Promotion get(@PathVariable UUID promotionId) {
    return promotionService.findById(promotionId);
  }
}
