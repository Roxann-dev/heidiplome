package hei.school.graduation.service.excel;

import hei.school.graduation.model.DiplomeEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class DiplomeExcelGenerator {

  private static final String[] HEADERS = {"Rang", "STD", "Nom", "Prénom", "Moyenne générale"};

  public File generate(List<DiplomeEntry> diplomes, String fileNameHint) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      XSSFSheet sheet = workbook.createSheet("Diplomés");

      writeHeaderRow(workbook, sheet);
      writeDataRows(sheet, diplomes);
      autoSizeColumns(sheet);

      File tempFile = Files.createTempFile(fileNameHint, ".xlsx").toFile();
      try (FileOutputStream out = new FileOutputStream(tempFile)) {
        workbook.write(out);
      }
      return tempFile;
    }
  }

  private void writeHeaderRow(XSSFWorkbook workbook, XSSFSheet sheet) {
    CellStyle headerStyle = workbook.createCellStyle();
    Font boldFont = workbook.createFont();
    boldFont.setBold(true);
    headerStyle.setFont(boldFont);

    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < HEADERS.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(HEADERS[i]);
      cell.setCellStyle(headerStyle);
    }
  }

  private void writeDataRows(XSSFSheet sheet, List<DiplomeEntry> diplomes) {
    int rowIndex = 1;
    for (DiplomeEntry entry : diplomes) {
      Row row = sheet.createRow(rowIndex++);
      row.createCell(0).setCellValue(entry.rang());
      row.createCell(1).setCellValue(entry.std());
      row.createCell(2).setCellValue(entry.nom());
      row.createCell(3).setCellValue(entry.prenom());
      row.createCell(4).setCellValue(entry.moyenneGenerale().doubleValue());
    }
  }

  private void autoSizeColumns(XSSFSheet sheet) {
    for (int i = 0; i < HEADERS.length; i++) {
      sheet.autoSizeColumn(i);
    }
  }
}
