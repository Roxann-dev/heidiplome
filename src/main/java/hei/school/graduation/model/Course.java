package hei.school.graduation.model;

import java.util.UUID;

public record Course(UUID id, String referenceCs, String title, Integer credits, UUID semesterId) {}
