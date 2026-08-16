package hei.school.graduation.dto;

import hei.school.graduation.model.Enum.ExamType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExamCreateRequest(
        @NotNull LocalDate examDate,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "Coefficient must be > 0")
        @DecimalMax(value = "1.0", inclusive = true, message = "Coefficient must be <= 1")
        BigDecimal coefficient,
        ExamType type // nullable: defaults to NORMAL in the service
) {}

