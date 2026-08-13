package hei.school.graduation.model;

import hei.school.graduation.model.Enum.Parcours;
import java.util.UUID;

public record AcademicGroup(UUID id, String reference, Parcours parcours, UUID semesterId) {}
