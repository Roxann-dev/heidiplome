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
    name = "course",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reference_cs", "semestre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEntity {
  @Id @UuidGenerator private UUID id;

  @Column(name = "reference_cs", nullable = false, length = 30)
  private String referenceCs;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false)
  private Integer credits;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "semestre_id", nullable = false)
  private SemesterEntity semester;
}
