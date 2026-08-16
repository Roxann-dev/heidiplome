package hei.school.graduation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseGroupAssignmentCreateRequest(@NotNull UUID groupId, @NotNull UUID semestreId) {}
