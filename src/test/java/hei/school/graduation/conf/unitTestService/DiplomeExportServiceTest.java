package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hei.school.graduation.file.bucket.BucketComponent;
import hei.school.graduation.model.DiplomeEntry;
import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.service.DiplomeListService;
import hei.school.graduation.service.excel.DiplomeExcelGenerator;
import hei.school.graduation.service.excel.DiplomeExportService;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiplomeExportServiceTest {

  @Mock private DiplomeListService diplomeListService;
  @Mock private DiplomeExcelGenerator diplomeExcelGenerator;
  @Mock private BucketComponent bucketComponent;

  private DiplomeExportService service;

  private UUID promotionId;
  private File fakeExcelFile;

  @BeforeEach
  void setUp() throws Exception {
    service = new DiplomeExportService(diplomeListService, diplomeExcelGenerator, bucketComponent);
    promotionId = UUID.randomUUID();

    fakeExcelFile = File.createTempFile("diplomes-test", ".xlsx");
    try (FileOutputStream fos = new FileOutputStream(fakeExcelFile)) {
      fos.write("dummy content".getBytes());
    }
    fakeExcelFile.deleteOnExit();
  }

  @Test
  void exportToExcel_shouldUploadToS3_andReturnFileBytes() throws Exception {
    List<DiplomeEntry> diplomes =
        List.of(new DiplomeEntry(1, "STD-001", "Rakoto", "Roxann", new BigDecimal("15.00")));

    when(diplomeListService.computeDiplomes(promotionId, Parcours.EL)).thenReturn(diplomes);
    when(diplomeExcelGenerator.generate(eq(diplomes), any(String.class))).thenReturn(fakeExcelFile);

    var result = service.exportToExcel(promotionId, Parcours.EL);

    assertThat(result.fileBytes()).isNotEmpty();
    assertThat(result.fileName()).isEqualTo("diplomes-EL.xlsx");
    verify(bucketComponent).upload(eq(fakeExcelFile), any(String.class));
  }

  @Test
  void exportToExcel_shouldDeleteTempFile_afterUpload() throws Exception {
    List<DiplomeEntry> diplomes = List.of();

    when(diplomeListService.computeDiplomes(promotionId, Parcours.TN)).thenReturn(diplomes);
    when(diplomeExcelGenerator.generate(eq(diplomes), any(String.class))).thenReturn(fakeExcelFile);

    service.exportToExcel(promotionId, Parcours.TN);

    assertThat(fakeExcelFile).doesNotExist();
  }

  @Test
  void exportToExcel_shouldUseBucketKeyContainingPromotionIdAndParcours() throws Exception {
    when(diplomeListService.computeDiplomes(promotionId, Parcours.EL)).thenReturn(List.of());
    when(diplomeExcelGenerator.generate(any(), any(String.class))).thenReturn(fakeExcelFile);

    service.exportToExcel(promotionId, Parcours.EL);

    verify(bucketComponent)
        .upload(
            eq(fakeExcelFile),
            org.mockito.ArgumentMatchers.argThat(
                key -> key.contains(promotionId.toString()) && key.contains("EL")));
  }
}
