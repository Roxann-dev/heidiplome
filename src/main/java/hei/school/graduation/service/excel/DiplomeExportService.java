package hei.school.graduation.service.excel;

import hei.school.graduation.file.bucket.BucketComponent;
import hei.school.graduation.model.DiplomeEntry;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.service.DiplomeListService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiplomeExportService {

  private final DiplomeListService diplomeListService;
  private final DiplomeExcelGenerator diplomeExcelGenerator;
  private final BucketComponent bucketComponent;

  public record ExportResult(byte[] fileBytes, String fileName) {}

  public ExportResult exportToExcel(UUID promotionId, Parcours parcours) throws IOException {
    List<DiplomeEntry> diplomes = diplomeListService.computeDiplomes(promotionId, parcours);

    String fileNameHint = "diplomes-" + parcours + "-" + promotionId;
    File excelFile = diplomeExcelGenerator.generate(diplomes, fileNameHint);

    try {
      String bucketKey = "diplomes/" + promotionId + "/" + parcours + ".xlsx";
      bucketComponent.upload(excelFile, bucketKey);

      byte[] fileBytes = Files.readAllBytes(excelFile.toPath());
      return new ExportResult(fileBytes, "diplomes-" + parcours + ".xlsx");
    } finally {
      Files.deleteIfExists(excelFile.toPath());
    }
  }
}
