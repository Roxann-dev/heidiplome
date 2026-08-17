package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.endpoint.event.EventProducer;
import hei.school.graduation.endpoint.event.model.ReleveGenerationRequested;
import hei.school.graduation.model.ReleveSemester;
import hei.school.graduation.service.ReleveService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ReleveController {

  private final EventProducer<ReleveGenerationRequested> eventProducer;
  private final ReleveService releveService;

  @PostMapping("/students/{studentId}/releve-pdf")
  public ResponseEntity<Map<String, String>> generateReleve(@PathVariable UUID studentId) {
    var event = ReleveGenerationRequested.builder().studentId(studentId).build();
    eventProducer.accept(List.of(event));
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("message", "Génération du relevé en cours, vous recevrez un email."));
  }

  @GetMapping("/students/{studentId}/releves/semestres/{semestreId}")
  public ReleveSemester getReleveSemestre(
      @PathVariable UUID studentId, @PathVariable UUID semestreId) {
    return releveService.getReleveSemestre(studentId, semestreId);
  }
}
