package hei.school.graduation.model;

import java.time.LocalDate;
import java.util.UUID;

public record StudentGroupAssignment(
        UUID id,
        UUID studentId,
        UUID groupId,
        UUID semestreId,
        LocalDate dateDebut,
        LocalDate dateFin) {}