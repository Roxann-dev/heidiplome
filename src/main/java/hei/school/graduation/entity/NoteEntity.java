package hei.school.graduation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "note",
    uniqueConstraints = @UniqueConstraint(columnNames = {"examen_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteEntity {
  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "examen_id", nullable = false)
  private ExamEntity exam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private UserEntity student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "saisie_par_id")
  private UserEntity enteredBy;

  @Column(name = "valeur", nullable = false, precision = 4, scale = 2)
  private BigDecimal value;

  @CreationTimestamp
  @Column(name = "date_saisie", nullable = false, updatable = false)
  private LocalDateTime entryDate;
}
