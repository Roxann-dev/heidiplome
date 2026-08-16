package hei.school.graduation.model;

import hei.school.graduation.model.Enum.Parcours;
import hei.school.graduation.model.Enum.StatutDiplome;
import java.math.BigDecimal;
import java.util.UUID;

public record ResultatStudent(
    UUID studentId,
    String std,
    String nom,
    String prenom,
    Parcours parcoursActuel,
    BigDecimal moyenneCumulee,
    StatutDiplome statut) {}
