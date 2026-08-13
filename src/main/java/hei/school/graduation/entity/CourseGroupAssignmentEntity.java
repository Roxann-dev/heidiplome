package hei.school.graduation.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "course_group_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseGroupAssignmentEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "semestre_id", nullable = false)
    private UUID semestreId;
}