package hei.school.graduation.model;

import hei.school.graduation.model.Enum.StatutReleve;
import java.math.BigDecimal;
import java.util.UUID;

public record CourseNoteLine(
    UUID courseId,
    String referenceCs,
    String title,
    int credits,
    BigDecimal average,
    StatutReleve status) {}
