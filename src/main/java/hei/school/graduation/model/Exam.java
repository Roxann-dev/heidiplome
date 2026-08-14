package hei.school.graduation.model;

import hei.school.graduation.model.Enum.ExamType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Exam(
    UUID id, UUID courseId, LocalDate examDate, BigDecimal coefficient, ExamType type) {}
