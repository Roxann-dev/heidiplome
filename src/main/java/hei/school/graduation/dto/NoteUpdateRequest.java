package hei.school.graduation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record NoteUpdateRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true, message = "Value must be >= 0")
        @DecimalMax(value = "20.0", inclusive = true, message = "Value must be <= 20")
        BigDecimal value,
        @NotBlank String reason) {}