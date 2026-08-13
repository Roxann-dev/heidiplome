package hei.school.graduation.model;

import java.util.UUID;

public record TeacherCourseAssignment(
        UUID id, UUID teacherId, UUID courseId, int anneeAcademique) {}