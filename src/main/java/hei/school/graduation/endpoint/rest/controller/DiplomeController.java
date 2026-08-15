package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.service.excel.DiplomeExportService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class DiplomeController {

  private static final MediaType XLSX_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final DiplomeExportService diplomeExportService;

  @GetMapping("/{promotionId}/diplomes/export")
  public ResponseEntity<byte[]> exportDiplomes(
      @PathVariable UUID promotionId, @RequestParam Parcours parcours) throws IOException {

    var result = diplomeExportService.exportToExcel(promotionId, parcours);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(XLSX_MEDIA_TYPE);
    headers.setContentDispositionFormData("attachment", result.fileName());

    return new ResponseEntity<>(result.fileBytes(), headers, HttpStatus.OK);
  }
}
