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
@Table(name = "teacher_course_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherCourseAssignmentEntity {

  @Id @UuidGenerator private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "annee_academique", nullable = false)
  private int anneeAcademique;
}
