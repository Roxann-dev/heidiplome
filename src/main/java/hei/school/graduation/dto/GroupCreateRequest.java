package hei.school.graduation.dto;

import hei.school.graduation.model.Enum.Parcours;
import jakarta.validation.constraints.NotBlank;

public record GroupCreateRequest(@NotBlank String reference, Parcours parcours) {}