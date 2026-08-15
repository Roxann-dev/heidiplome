package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hei.school.graduation.endpoint.event.model.ReleveGenerationRequested;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.file.bucket.BucketComponent;
import hei.school.graduation.mail.Email;
import hei.school.graduation.mail.Mailer;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.UserRepository;
import hei.school.graduation.service.event.ReleveGenerationRequestedService;
import hei.school.graduation.service.pdf.ReleasePdfGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleveGenerationRequestedServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ReleasePdfGenerator releasePdfGenerator;
  @Mock private BucketComponent bucketComponent;
  @Mock private Mailer mailer;

  private ReleveGenerationRequestedService service;

  private UUID studentId;
  private UserEntity student;
  private File fakePdfFile;

  @BeforeEach
  void setUp() throws IOException {
    service =
        new ReleveGenerationRequestedService(
            userRepository, releasePdfGenerator, bucketComponent, mailer);
    studentId = UUID.randomUUID();
    student =
        UserEntity.builder()
            .id(studentId)
            .reference("STD-2023-001")
            .firstName("Roxann")
            .lastName("Rakoto")
            .email("roxann@example.com")
            .role(UserRole.STUDENT)
            .build();
    fakePdfFile = File.createTempFile("releve-test", ".pdf");
    fakePdfFile.deleteOnExit();
  }

  @Test
  void accept_shouldGeneratePdf_uploadToS3_andSendEmail_whenStudentExists() throws IOException {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(releasePdfGenerator.generate(student)).thenReturn(fakePdfFile);

    var event = ReleveGenerationRequested.builder().studentId(studentId).build();

    service.accept(event);

    verify(bucketComponent).upload(eq(fakePdfFile), any(String.class));
    verify(mailer).accept(any(Email.class));
  }

  @Test
  void accept_shouldUploadWithBucketKeyContainingStudentId() throws IOException {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(releasePdfGenerator.generate(student)).thenReturn(fakePdfFile);

    var event = ReleveGenerationRequested.builder().studentId(studentId).build();

    service.accept(event);

    verify(bucketComponent)
        .upload(
            eq(fakePdfFile),
            org.mockito.ArgumentMatchers.argThat(key -> key.contains(studentId.toString())));
  }

  @Test
  void accept_shouldSendEmailToStudentAddress_withPdfAttached() throws IOException {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(releasePdfGenerator.generate(student)).thenReturn(fakePdfFile);

    var event = ReleveGenerationRequested.builder().studentId(studentId).build();

    service.accept(event);

    verify(mailer)
        .accept(
            org.mockito.ArgumentMatchers.argThat(
                email ->
                    email.to().getAddress().equals(student.getEmail())
                        && email.attachments().contains(fakePdfFile)
                        && email.subject().contains("relevé")));
  }

  @Test
  void accept_shouldThrowNotFound_whenStudentDoesNotExist() {
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    var event = ReleveGenerationRequested.builder().studentId(studentId).build();

    assertThatThrownBy(() -> service.accept(event))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(studentId.toString());

    org.mockito.Mockito.verifyNoInteractions(releasePdfGenerator, bucketComponent, mailer);
  }
}
