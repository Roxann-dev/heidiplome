package hei.school.graduation.conf.unitTestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.service.calculator.AnnualAverageCalculator;
import hei.school.graduation.service.pdf.ReleasePdfGenerator;
import java.io.File;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleasePdfGeneratorTest {

  @Mock private AnnualAverageCalculator annualAverageCalculator;

  private ReleasePdfGenerator generator;

  private UserEntity student;

  @BeforeEach
  void setUp() {
    generator = new ReleasePdfGenerator(annualAverageCalculator);
    student =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .reference("STD-2023-001")
            .firstName("Roxann")
            .lastName("Rakoto")
            .email("roxann@example.com")
            .role(UserRole.STUDENT)
            .build();
  }

  @Test
  void generate_shouldProduceNonEmptyPdfFile_whenAllYearsAreComplete() throws Exception {
    when(annualAverageCalculator.compute(student.getId(), 1))
        .thenReturn(
            new AnnualAverageCalculator.AnnualAverageResult(new BigDecimal("14.50"), 60, true));
    when(annualAverageCalculator.compute(student.getId(), 2))
        .thenReturn(
            new AnnualAverageCalculator.AnnualAverageResult(new BigDecimal("13.00"), 60, true));
    when(annualAverageCalculator.compute(student.getId(), 3))
        .thenReturn(
            new AnnualAverageCalculator.AnnualAverageResult(new BigDecimal("15.20"), 60, true));

    File pdfFile = generator.generate(student);

    assertThat(pdfFile).exists();
    assertThat(pdfFile.length()).isGreaterThan(0);
    assertThat(pdfFile.getName()).endsWith(".pdf");

    pdfFile.deleteOnExit();
  }

  @Test
  void generate_shouldProducePdf_whenSomeYearsAreProvisoire() throws Exception {
    when(annualAverageCalculator.compute(student.getId(), 1))
        .thenReturn(
            new AnnualAverageCalculator.AnnualAverageResult(new BigDecimal("12.00"), 40, false));
    when(annualAverageCalculator.compute(student.getId(), 2))
        .thenReturn(new AnnualAverageCalculator.AnnualAverageResult(BigDecimal.ZERO, 0, true));
    when(annualAverageCalculator.compute(student.getId(), 3))
        .thenReturn(new AnnualAverageCalculator.AnnualAverageResult(BigDecimal.ZERO, 0, true));

    File pdfFile = generator.generate(student);

    assertThat(pdfFile).exists();
    assertThat(pdfFile.length()).isGreaterThan(0);

    pdfFile.deleteOnExit();
  }

  @Test
  void generate_shouldCallCalculatorForAllThreeYears() throws Exception {
    when(annualAverageCalculator.compute(student.getId(), 1))
        .thenReturn(new AnnualAverageCalculator.AnnualAverageResult(BigDecimal.ZERO, 0, true));
    when(annualAverageCalculator.compute(student.getId(), 2))
        .thenReturn(new AnnualAverageCalculator.AnnualAverageResult(BigDecimal.ZERO, 0, true));
    when(annualAverageCalculator.compute(student.getId(), 3))
        .thenReturn(new AnnualAverageCalculator.AnnualAverageResult(BigDecimal.ZERO, 0, true));

    generator.generate(student);

    org.mockito.Mockito.verify(annualAverageCalculator).compute(student.getId(), 1);
    org.mockito.Mockito.verify(annualAverageCalculator).compute(student.getId(), 2);
    org.mockito.Mockito.verify(annualAverageCalculator).compute(student.getId(), 3);
  }
}
