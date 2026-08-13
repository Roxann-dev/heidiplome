CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role   AS ENUM ('STUDENT', 'TEACHER', 'ADMIN');
CREATE TYPE parcours    AS ENUM ('EL', 'TN');
CREATE TYPE examen_type AS ENUM ('NORMAL', 'RATTRAPAGE');

CREATE TABLE app_user (
                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          reference   VARCHAR(50)  NOT NULL UNIQUE,
                          last_name   VARCHAR(100) NOT NULL,
                          first_name  VARCHAR(100) NOT NULL,
                          role        user_role    NOT NULL,
                          email       VARCHAR(255) NOT NULL UNIQUE,
                          created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_user_role ON app_user (role);

CREATE TABLE promotion (
                           id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           libelle        VARCHAR(100) NOT NULL,
                           annee_entree   INTEGER      NOT NULL,
                           created_at     TIMESTAMP    NOT NULL DEFAULT now(),

                           CONSTRAINT uq_promotion_annee UNIQUE (annee_entree)
);

CREATE TABLE semestre (
                          id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          promotion_id   UUID         NOT NULL REFERENCES promotion (id) ON DELETE CASCADE,
                          numero         INTEGER      NOT NULL,
                          annee_cursus   INTEGER      NOT NULL,

                          CONSTRAINT chk_semestre_numero CHECK (numero BETWEEN 1 AND 6),
                          CONSTRAINT chk_semestre_annee  CHECK (annee_cursus BETWEEN 1 AND 3),
                          CONSTRAINT uq_semestre_promo_numero UNIQUE (promotion_id, numero)
);

CREATE INDEX idx_semestre_promotion ON semestre (promotion_id);

CREATE TABLE academic_group (
                                id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                reference    VARCHAR(20)  NOT NULL,
                                parcours     parcours,
                                semestre_id  UUID         NOT NULL REFERENCES semestre (id) ON DELETE CASCADE,

                                CONSTRAINT uq_group_ref_semestre UNIQUE (reference, semestre_id)
);

CREATE INDEX idx_group_semestre ON academic_group (semestre_id);

CREATE TABLE course (
                        id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        reference_cs   VARCHAR(30)  NOT NULL,
                        title          VARCHAR(200) NOT NULL,
                        credits        INTEGER      NOT NULL,
                        semestre_id    UUID         NOT NULL REFERENCES semestre (id) ON DELETE CASCADE,

                        CONSTRAINT chk_course_credits CHECK (credits > 0),
                        CONSTRAINT uq_course_ref_semestre UNIQUE (reference_cs, semestre_id)
);

CREATE INDEX idx_course_semestre ON course (semestre_id);

CREATE TABLE examen (
                        id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        course_id      UUID          NOT NULL REFERENCES course (id) ON DELETE CASCADE,
                        date_examen    DATE          NOT NULL,
                        coefficient    NUMERIC(3,2)  NOT NULL,
                        type           examen_type   NOT NULL DEFAULT 'NORMAL',

                        CONSTRAINT chk_examen_coef CHECK (coefficient > 0 AND coefficient <= 1)
);

CREATE INDEX idx_examen_course ON examen (course_id);

CREATE TABLE note (
                      id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      examen_id      UUID          NOT NULL REFERENCES examen (id) ON DELETE CASCADE,
                      student_id     UUID          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
                      saisie_par_id  UUID          REFERENCES app_user (id) ON DELETE SET NULL,
                      valeur         NUMERIC(4,2)  NOT NULL,
                      date_saisie    TIMESTAMP     NOT NULL DEFAULT now(),

                      CONSTRAINT chk_note_valeur CHECK (valeur >= 0 AND valeur <= 20),
                      CONSTRAINT uq_note_examen_student UNIQUE (examen_id, student_id)
);

CREATE INDEX idx_note_student ON note (student_id);
CREATE INDEX idx_note_examen  ON note (examen_id);

CREATE TABLE note_history (
                              id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              note_id          UUID          NOT NULL REFERENCES note (id) ON DELETE CASCADE,
                              ancienne_valeur  NUMERIC(4,2)  NOT NULL,
                              date_modif       TIMESTAMP     NOT NULL DEFAULT now(),
                              modifie_par_id   UUID          NOT NULL REFERENCES app_user (id),
                              motif            TEXT          NOT NULL,

                              CONSTRAINT chk_note_history_valeur CHECK (ancienne_valeur >= 0 AND ancienne_valeur <= 20)
);

CREATE INDEX idx_note_history_note ON note_history (note_id);

CREATE TABLE student_group_assignment (
                                          id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          student_id     UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
                                          group_id       UUID NOT NULL REFERENCES academic_group (id) ON DELETE CASCADE,
                                          semestre_id    UUID NOT NULL REFERENCES semestre (id) ON DELETE CASCADE,
                                          date_debut     DATE NOT NULL,
                                          date_fin       DATE,

                                          CONSTRAINT uq_sga_student_semestre UNIQUE (student_id, semestre_id)
);

CREATE INDEX idx_sga_student  ON student_group_assignment (student_id);
CREATE INDEX idx_sga_group    ON student_group_assignment (group_id);
CREATE INDEX idx_sga_semestre ON student_group_assignment (semestre_id);

CREATE TABLE course_group_assignment (
                                         id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         course_id    UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
                                         group_id     UUID NOT NULL REFERENCES academic_group (id) ON DELETE CASCADE,
                                         semestre_id  UUID NOT NULL REFERENCES semestre (id) ON DELETE CASCADE,

                                         CONSTRAINT uq_cga_course_group UNIQUE (course_id, group_id)
);

CREATE INDEX idx_cga_course   ON course_group_assignment (course_id);
CREATE INDEX idx_cga_group    ON course_group_assignment (group_id);
CREATE INDEX idx_cga_semestre ON course_group_assignment (semestre_id);

CREATE TABLE teacher_course_assignment (
                                           id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           teacher_id        UUID    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
                                           course_id         UUID    NOT NULL REFERENCES course (id) ON DELETE CASCADE,
                                           annee_academique  INTEGER NOT NULL,

                                           CONSTRAINT uq_tca_teacher_course_annee UNIQUE (teacher_id, course_id, annee_academique)
);

CREATE INDEX idx_tca_teacher ON teacher_course_assignment (teacher_id);
CREATE INDEX idx_tca_course  ON teacher_course_assignment (course_id);
