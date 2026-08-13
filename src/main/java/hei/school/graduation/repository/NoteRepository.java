package hei.school.graduation.repository;

import hei.school.graduation.entity.NoteEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<NoteEntity, UUID> {}
