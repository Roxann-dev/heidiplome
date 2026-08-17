package hei.school.graduation.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReleveAnnuel(
    UUID studentId,
    int cursusYear,
    BigDecimal generalAverage,
    int totalCredits,
    List<CourseNoteLine> lines) {}
