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
@Table(
    name = "teacher_course_assignment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"teacher_id", "course_id", "annee_academique"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherCourseAssignmentEntity {

  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", nullable = false)
  private UserEntity teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private CourseEntity course;

  @Column(name = "annee_academique", nullable = false)
  private int anneeAcademique;
}
