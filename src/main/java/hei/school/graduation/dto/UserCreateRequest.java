package hei.school.graduation.dto;

import hei.school.graduation.model.Enum.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank String reference,
    @NotBlank String lastName,
    @NotBlank String firstName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
        String password,
    @NotNull UserRole role) {}
