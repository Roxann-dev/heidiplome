package hei.school.graduation.model;

import hei.school.graduation.model.Enum.UserRole;
import java.util.UUID;

public record User(
    UUID id, String reference, String firstName, String lastName, String email, UserRole role) {}
