package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;

import hei.school.graduation.model.DiplomeEntry;
import hei.school.graduation.service.excel.DiplomeExcelGenerator;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class DiplomeExcelGeneratorTest {

  private final DiplomeExcelGenerator generator = new DiplomeExcelGenerator();

  @Test
  void generate_shouldCreateFileWithHeaderAndDataRows() throws Exception {
    List<DiplomeEntry> diplomes =
        List.of(
            new DiplomeEntry(1, "STD-001", "Rakoto", "Roxann", new BigDecimal("15.50")),
            new DiplomeEntry(2, "STD-002", "Rabe", "Andry", new BigDecimal("14.00")));

    File file = generator.generate(diplomes, "diplomes-test");

    assertThat(file).exists();
    assertThat(file.length()).isGreaterThan(0);

    try (FileInputStream fis = new FileInputStream(file);
        XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
      XSSFSheet sheet = workbook.getSheet("Diplomés");
      assertThat(sheet).isNotNull();

      var headerRow = sheet.getRow(0);
      assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Rang");
      assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("STD");
      assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Nom");
      assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Prénom");
      assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Moyenne générale");

      var row1 = sheet.getRow(1);
      assertThat(row1.getCell(0).getNumericCellValue()).isEqualTo(1);
      assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("STD-001");
      assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("Rakoto");
      assertThat(row1.getCell(3).getStringCellValue()).isEqualTo("Roxann");
      assertThat(row1.getCell(4).getNumericCellValue()).isEqualTo(15.50);

      var row2 = sheet.getRow(2);
      assertThat(row2.getCell(1).getStringCellValue()).isEqualTo("STD-002");
    }

    file.deleteOnExit();
  }

  @Test
  void generate_shouldCreateFileWithHeaderOnly_whenListIsEmpty() throws Exception {
    File file = generator.generate(List.of(), "diplomes-empty");

    assertThat(file).exists();

    try (FileInputStream fis = new FileInputStream(file);
        XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
      XSSFSheet sheet = workbook.getSheet("Diplomés");
      assertThat(sheet.getLastRowNum()).isZero();
    }

    file.deleteOnExit();
  }
}
