package hei.school.graduation.repository;

import hei.school.graduation.entity.NoteHistoryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteHistoryRepository extends JpaRepository<NoteHistoryEntity, UUID> {}
