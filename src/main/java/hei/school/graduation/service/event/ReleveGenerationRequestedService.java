package hei.school.graduation.service.event;

import hei.school.graduation.endpoint.event.model.ReleveGenerationRequested;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.file.bucket.BucketComponent;
import hei.school.graduation.mail.Email;
import hei.school.graduation.mail.Mailer;
import hei.school.graduation.repository.UserRepository;
import hei.school.graduation.service.pdf.ReleasePdfGenerator;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReleveGenerationRequestedService implements Consumer<ReleveGenerationRequested> {

  private final UserRepository userRepository;
  private final ReleasePdfGenerator releasePdfGenerator;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(ReleveGenerationRequested releveGenerationRequested) {
    UserEntity student =
        userRepository
            .findById(releveGenerationRequested.getStudentId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Student not found: " + releveGenerationRequested.getStudentId()));

    File pdfFile = releasePdfGenerator.generate(student);

    String bucketKey =
        "releves/" + student.getId() + "/releve-" + System.currentTimeMillis() + ".pdf";
    bucketComponent.upload(pdfFile, bucketKey);

    var recipientAddress = new InternetAddress(student.getEmail());
    mailer.accept(
        new Email(
            recipientAddress,
            List.of(),
            List.of(),
            "Votre relevé de notes complet",
            "Bonjour "
                + student.getFirstName()
                + ", veuillez trouver ci-joint votre relevé de notes complet.",
            List.of(pdfFile)));
  }
}
