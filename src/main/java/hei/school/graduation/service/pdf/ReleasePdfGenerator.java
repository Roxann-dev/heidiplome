package hei.school.graduation.service.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import hei.school.graduation.entity.CourseEntity;
import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.SemesterEntity;
import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.ExamType;
import hei.school.graduation.model.Enum.StatutReleve;
import hei.school.graduation.repository.CourseRepository;
import hei.school.graduation.repository.ExamRepository;
import hei.school.graduation.repository.NoteRepository;
import hei.school.graduation.repository.SemesterRepository;
import hei.school.graduation.repository.StudentGroupAssignmentRepository;
import hei.school.graduation.service.GroupIsolationService;
import hei.school.graduation.service.calculator.AnnualAverageCalculator;
import hei.school.graduation.service.calculator.CourseAverageCalculator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReleasePdfGenerator {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final AnnualAverageCalculator annualAverageCalculator;
  private final StudentGroupAssignmentRepository studentGroupAssignmentRepository;
  private final SemesterRepository semesterRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final NoteRepository noteRepository;
  private final GroupIsolationService groupIsolationService;
  private final CourseAverageCalculator courseAverageCalculator;

  @Transactional(readOnly = true)
  public File generate(UserEntity student) throws IOException {
    UUID promotionId = resolvePromotionId(student.getId());
    StringBuilder html = new StringBuilder();
    appendHeader(html, student);

    for (int anneeCursus = 1; anneeCursus <= 3; anneeCursus++) {
      appendYearSection(html, student.getId(), promotionId, anneeCursus);
    }

    html.append("</body></html>");

    return renderToPdf(html.toString(), student.getId());
  }

  private void appendHeader(StringBuilder html, UserEntity student) {
    html.append("<html><head><meta charset=\"UTF-8\"/><style>")
        .append(CSS)
        .append("</style></head><body>");
    html.append("<div class=\"header\">");
    html.append("<h1>Relevé de notes complet</h1>");
    html.append("<p class=\"student\">")
        .append(escape(student.getFirstName()))
        .append(" ")
        .append(escape(student.getLastName()))
        .append(" &#8212; ")
        .append(escape(student.getReference()))
        .append("</p>");
    html.append("</div>");
  }

  private void appendYearSection(
      StringBuilder html, UUID studentId, UUID promotionId, int anneeCursus) {
    List<SemesterEntity> semestres =
        semesterRepository.findByPromotion_IdAndCursusYear(promotionId, anneeCursus);

    var annual = annualAverageCalculator.compute(studentId, anneeCursus);

    html.append("<div class=\"year\">");
    html.append("<h2>Année ").append(anneeCursus).append("</h2>");

    for (SemesterEntity semestre : semestres) {
      appendSemesterTable(html, studentId, semestre);
    }

    html.append("<div class=\"year-summary ")
        .append(annual.complet() ? "status-complet" : "status-provisoire")
        .append("\">");
    html.append("<span>Moyenne générale de l'année : <strong>")
        .append(formatNumber(annual.moyenneGenerale()))
        .append("/20</strong></span>");
    html.append("<span>Crédits validés : <strong>")
        .append(annual.totalCredits())
        .append("</strong></span>");
    html.append("<span class=\"badge\">")
        .append(annual.complet() ? "COMPLET" : "PROVISOIRE")
        .append("</span>");
    html.append("</div>");

    html.append("</div>");
  }

  private void appendSemesterTable(StringBuilder html, UUID studentId, SemesterEntity semestre) {
    List<UUID> courseIds =
        groupIsolationService.resolveFollowedCourseIdsIfAssigned(studentId, semestre.getId());

    html.append("<h3>Semestre ").append(semestre.getNumber()).append("</h3>");

    if (courseIds.isEmpty()) {
      html.append("<p class=\"empty\">Aucune donnée disponible pour ce semestre.</p>");
      return;
    }

    html.append("<table class=\"courses\">");
    html.append(
        "<thead><tr><th>Cours</th><th>Crédits</th><th>Examens</th><th>Moyenne</th><th>Statut</th></tr></thead>");
    html.append("<tbody>");

    for (UUID courseId : courseIds) {
      appendCourseRow(html, studentId, courseId);
    }

    html.append("</tbody></table>");
  }

  private void appendCourseRow(StringBuilder html, UUID studentId, UUID courseId) {
    CourseEntity course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new NotFoundException("Course introuvable : " + courseId));

    List<ExamEntity> exams = examRepository.findByCourseId(courseId);
    List<UUID> examIds = exams.stream().map(ExamEntity::getId).toList();
    List<NoteEntity> notes = noteRepository.findByStudent_IdAndExam_IdIn(studentId, examIds);
    Map<UUID, NoteEntity> noteByExam =
        notes.stream().collect(Collectors.toMap(n -> n.getExam().getId(), n -> n));

    var result = courseAverageCalculator.compute(courseId, studentId);

    String examsDetail =
        exams.stream()
            .map(
                exam -> {
                  NoteEntity note = noteByExam.get(exam.getId());
                  String label =
                      exam.getType() == ExamType.RATTRAPAGE
                          ? "Rattrapage"
                          : exam.getExamDate().format(DATE_FORMAT);
                  String valeur =
                      note != null ? formatNumber(note.getValue()) + "/20" : "en attente";
                  return label + " (coef " + formatNumber(exam.getCoefficient()) + ") : " + valeur;
                })
            .collect(Collectors.joining("<br/>"));

    html.append("<tr>");
    html.append("<td>")
        .append(escape(course.getReferenceCs()))
        .append(" &#8212; ")
        .append(escape(course.getTitle()))
        .append("</td>");
    html.append("<td>").append(course.getCredits()).append("</td>");
    html.append("<td class=\"exams\">").append(examsDetail).append("</td>");
    html.append("<td>")
        .append(result.moyenne() != null ? formatNumber(result.moyenne()) + "/20" : "-")
        .append("</td>");
    html.append("<td><span class=\"badge-small ")
        .append(result.statut() == StatutReleve.COMPLET ? "status-complet" : "status-provisoire")
        .append("\">")
        .append(result.statut())
        .append("</span></td>");
    html.append("</tr>");
  }

  private File renderToPdf(String htmlContent, UUID studentId) throws IOException {
    File pdfFile = File.createTempFile("releve-" + studentId, ".pdf");
    try (ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileOutputStream fos = new FileOutputStream(pdfFile)) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(htmlContent, null);
      builder.toStream(os);
      builder.run();
      fos.write(os.toByteArray());
    }
    return pdfFile;
  }

  private UUID resolvePromotionId(UUID studentId) {
    StudentGroupAssignmentEntity anyAssignment =
        studentGroupAssignmentRepository.findByStudent_IdOrderByDateDebutAsc(studentId).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new NotFoundException("Aucune affectation de group pour student " + studentId));
    return anyAssignment.getSemestre().getPromotion().getId();
  }

  private String formatNumber(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private String escape(String value) {
    return value == null
        ? ""
        : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static final String CSS =
      "body{font-family:Helvetica,Arial,sans-serif;color:#222;margin:32px;}"
          + "h1{font-size:20px;margin-bottom:4px;color:#1a2b4c;}"
          + ".student{font-size:13px;color:#555;margin-top:0;}.year{margin-top:24px;border-top:2px"
          + " solid #1a2b4c;padding-top:8px;}"
          + "h2{font-size:16px;color:#1a2b4c;margin-bottom:4px;}h3{font-size:13px;color:#333;margin:12px"
          + " 0 4px;}"
          + "table.courses{width:100%;border-collapse:collapse;font-size:11px;margin-bottom:8px;}table.courses"
          + " th{background:#1a2b4c;color:#fff;text-align:left;padding:6px;}table.courses"
          + " td{border-bottom:1px solid #ddd;padding:6px;vertical-align:top;}"
          + "td.exams{font-size:10px;color:#444;}"
          + ".empty{font-size:11px;color:#888;font-style:italic;}"
          + ".year-summary{display:block;font-size:12px;margin-top:6px;padding:8px;border-radius:4px;background:#f2f2f2;}.year-summary"
          + " span{margin-right:16px;}.badge{padding:2px"
          + " 8px;border-radius:3px;color:#fff;font-weight:bold;}.badge-small{padding:1px"
          + " 6px;border-radius:3px;color:#fff;font-size:10px;}.status-complet{background:#2e7d32;}"
          + ".status-provisoire{background:#c9871f;}";
}
