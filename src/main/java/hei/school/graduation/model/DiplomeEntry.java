package hei.school.graduation.model;

import java.math.BigDecimal;

public record DiplomeEntry(
    int rang, String std, String nom, String prenom, BigDecimal moyenneGenerale) {}
