package hei.school.graduation.repository;

import hei.school.graduation.entity.NoteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {
  List<NoteEntity> findByStudent_IdAndExam_IdIn(UUID studentId, List<UUID> examenIds);

  boolean existsByExam_IdAndStudent_Id(UUID examenId, UUID studentId);
  List<NoteEntity> findByExam_Id(UUID examenId);
}
