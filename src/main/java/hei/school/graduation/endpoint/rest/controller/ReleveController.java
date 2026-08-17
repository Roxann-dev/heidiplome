package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.endpoint.event.EventProducer;
import hei.school.graduation.endpoint.event.model.ReleveGenerationRequested;
import hei.school.graduation.model.ReleveAnnuel;
import hei.school.graduation.model.ReleveSemester;
import hei.school.graduation.service.ReleveService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/students")
public class ReleveController {

  private final EventProducer<ReleveGenerationRequested> eventProducer;
  private final ReleveService releveService;

  @PostMapping("/{studentId}/releve-pdf")
  public ResponseEntity<Map<String, String>> generateReleve(@PathVariable UUID studentId) {
    var event = ReleveGenerationRequested.builder().studentId(studentId).build();
    eventProducer.accept(List.of(event));
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("message", "Transcript generation in progress, you will receive an email."));
  }

  @GetMapping("/{studentId}/releves/semestres/{semestreId}")
  public ReleveSemester getReleveSemester(
      @PathVariable UUID studentId, @PathVariable UUID semestreId) {
    return releveService.getReleveSemestre(studentId, semestreId);
  }

  @GetMapping("/{studentId}/releves/years/{cursusYear}")
  public ReleveAnnuel getReleveYear(@PathVariable UUID studentId, @PathVariable int cursusYear) {
    return releveService.getReleveAnnuel(studentId, cursusYear);
  }
}
