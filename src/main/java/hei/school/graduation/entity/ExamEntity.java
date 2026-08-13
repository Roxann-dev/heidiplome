package hei.school.graduation.entity;

import hei.school.graduation.model.Enum.ExamType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "examen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamEntity {
  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private CourseEntity course;

  @Column(name = "date_examen", nullable = false)
  private LocalDate examDate;

  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal coefficient;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "examen_type")
  private ExamType type;
}
