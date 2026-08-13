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
@Table(name = "semestre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterEntity {
  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promotion_id", nullable = false)
  private PromotionEntity promotion;

  @Column(name = "numero", nullable = false)
  private Integer number;

  @Column(name = "annee_cursus", nullable = false)
  private Integer cursusYear;
}
