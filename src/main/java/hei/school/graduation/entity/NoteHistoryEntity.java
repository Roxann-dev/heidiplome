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
@Table(name = "note_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteHistoryEntity {
  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "note_id", nullable = false)
  private NoteEntity note;

  @Column(name = "ancienne_valeur", nullable = false, precision = 4, scale = 2)
  private BigDecimal previousValue;

  @CreationTimestamp
  @Column(name = "date_modif", nullable = false, updatable = false)
  private LocalDateTime modificationDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modifie_par_id", nullable = false)
  private UserEntity modifiedBy;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String reason;
}
