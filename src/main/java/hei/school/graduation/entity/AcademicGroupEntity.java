package hei.school.graduation.entity;

import hei.school.graduation.model.Enum.Parcours;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "academic_group",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reference", "semestre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicGroupEntity {
  @Id @UuidGenerator private UUID id;

  @Column(nullable = false, length = 20)
  private String reference;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "parcours")
  private Parcours parcours;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "semestre_id", nullable = false)
  private SemesterEntity semester;
}
