package hei.school.graduation.mapper;

import hei.school.graduation.entity.StudentGroupAssignmentEntity;
import hei.school.graduation.model.StudentGroupAssignment;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupAssignmentMapper {

    public StudentGroupAssignment toDomain(StudentGroupAssignmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StudentGroupAssignment(
                entity.getId(),
                entity.getStudentId(),
                entity.getGroupId(),
                entity.getSemestreId(),
                entity.getDateDebut(),
                entity.getDateFin());
    }

    public StudentGroupAssignmentEntity toEntity(StudentGroupAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return StudentGroupAssignmentEntity.builder()
                .id(assignment.id())
                .studentId(assignment.studentId())
                .groupId(assignment.groupId())
                .semestreId(assignment.semestreId())
                .dateDebut(assignment.dateDebut())
                .dateFin(assignment.dateFin())
                .build();
    }
}