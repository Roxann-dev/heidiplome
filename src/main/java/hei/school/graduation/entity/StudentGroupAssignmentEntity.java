package hei.school.graduation.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "student_group_assignment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "semestre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroupAssignmentEntity {

  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private UserEntity student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private AcademicGroupEntity group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "semestre_id", nullable = false)
  private SemesterEntity semestre;

  @Column(name = "date_debut", nullable = false)
  private LocalDate dateDebut;

  @Column(name = "date_fin")
  private LocalDate dateFin;
}
