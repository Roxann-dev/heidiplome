package hei.school.graduation.model;

import java.util.UUID;

public record CourseGroupAssignment(
        UUID id, UUID courseId, UUID groupId, UUID semestreId) {}