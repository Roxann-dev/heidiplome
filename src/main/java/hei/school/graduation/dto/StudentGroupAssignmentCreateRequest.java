package hei.school.graduation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record StudentGroupAssignmentCreateRequest(
        @NotNull UUID groupId,
        @NotNull UUID semestreId,
        @NotNull LocalDate dateDebut,
        LocalDate dateFin) {}
