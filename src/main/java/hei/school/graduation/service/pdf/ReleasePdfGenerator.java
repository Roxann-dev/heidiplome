package hei.school.graduation.service.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.service.calculator.AnnualAverageCalculator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReleasePdfGenerator {

  private final AnnualAverageCalculator annualAverageCalculator;

  public File generate(UserEntity student) throws IOException {
    StringBuilder html = new StringBuilder();
    html.append("<html><body>");
    html.append("<h1>Relevé de notes complet</h1>");
    html.append("<p>")
        .append(student.getFirstName())
        .append(" ")
        .append(student.getLastName())
        .append(" (")
        .append(student.getReference())
        .append(")</p>");

    for (int anneeCursus = 1; anneeCursus <= 3; anneeCursus++) {
      var result = annualAverageCalculator.compute(student.getId(), anneeCursus);
      html.append("<h2>Année ").append(anneeCursus).append("</h2>");
      html.append("<p>Moyenne générale : ")
          .append(result.moyenneGenerale())
          .append(" (")
          .append(result.totalCredits())
          .append(" crédits) - ")
          .append(result.complet() ? "COMPLET" : "PROVISOIRE")
          .append("</p>");
    }

    html.append("</body></html>");

    File pdfFile = File.createTempFile("releve-" + student.getId(), ".pdf");
    try (ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileOutputStream fos = new FileOutputStream(pdfFile)) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html.toString(), null);
      builder.toStream(os);
      builder.run();
      fos.write(os.toByteArray());
    }

    return pdfFile;
  }
}
