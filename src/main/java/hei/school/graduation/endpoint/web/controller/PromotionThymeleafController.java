package hei.school.graduation.endpoint.web.controller;

import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.repository.PromotionRepository;
import hei.school.graduation.service.DiplomeListService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PromotionThymeleafController {

  private final PromotionRepository promotionRepository;
  private final DiplomeListService diplomeListService;

  @GetMapping("/promotions-view")
  public String listPromotions(Model model) {
    model.addAttribute("promotions", promotionRepository.findAll());
    return "promotions";
  }

  @GetMapping("/promotions/{promotionId}/diplomes/view")
  public String viewDiplomes(
      @PathVariable UUID promotionId, @RequestParam Parcours parcours, Model model) {
    var promotion = promotionRepository.findById(promotionId).orElseThrow();
    model.addAttribute("diplomes", diplomeListService.computeDiplomes(promotionId, parcours));
    model.addAttribute("parcours", parcours.name());
    model.addAttribute("promotionLabel", promotion.getLabel());
    return "diplomes";
  }
}
