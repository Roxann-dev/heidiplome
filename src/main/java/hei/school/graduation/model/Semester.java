package hei.school.graduation.model;

import java.util.UUID;

public record Semester(UUID id, UUID promotionId, Integer number, Integer cursusYear) {}
