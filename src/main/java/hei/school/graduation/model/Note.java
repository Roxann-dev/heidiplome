package hei.school.graduation.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record Note(
    UUID id,
    UUID examId,
    UUID studentId,
    UUID enteredById,
    BigDecimal value,
    LocalDateTime entryDate) {}
