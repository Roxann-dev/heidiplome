package hei.school.graduation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TeacherCourseAssignmentCreateRequest(
    @NotNull UUID teacherId, @NotNull UUID courseId, @NotNull Integer anneeAcademique) {}
