package hei.school.graduation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseCreateRequest(
        @NotBlank String referenceCs,
        @NotBlank String title,
        @NotNull @Min(1) Integer credits,
        @NotNull UUID semestreId) {}