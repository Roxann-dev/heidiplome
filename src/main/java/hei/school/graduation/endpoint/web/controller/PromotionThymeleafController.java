package hei.school.graduation.endpoint.web.controller;

import hei.school.graduation.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PromotionThymeleafController {

  private final PromotionRepository promotionRepository;

  @GetMapping("/promotions-view")
  public String listPromotions(Model model) {
    model.addAttribute("promotions", promotionRepository.findAll());
    return "promotions";
  }
}
