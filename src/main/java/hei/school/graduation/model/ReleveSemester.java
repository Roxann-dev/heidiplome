package hei.school.graduation.model;

import hei.school.graduation.model.Enum.StatutReleve;
import java.util.List;
import java.util.UUID;

public record ReleveSemestre(
        UUID studentId,
        UUID semestreId,
        UUID groupId,
        List<CourseNoteLine> lines,
        StatutReleve globalStatus) {}