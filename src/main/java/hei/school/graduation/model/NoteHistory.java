package hei.school.graduation.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record NoteHistory(
    UUID id,
    UUID noteId,
    BigDecimal previousValue,
    LocalDateTime modificationDate,
    UUID modifiedById,
    String reason) {}
