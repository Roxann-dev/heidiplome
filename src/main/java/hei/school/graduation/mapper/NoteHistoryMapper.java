package hei.school.graduation.mapper;

import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.NoteHistoryEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.NoteHistory;
import org.springframework.stereotype.Component;

@Component
public class NoteHistoryMapper {

  public NoteHistory toDomain(NoteHistoryEntity entity) {
    if (entity == null) {
      return null;
    }
    return new NoteHistory(
        entity.getId(),
        entity.getNote().getId(),
        entity.getPreviousValue(),
        entity.getModificationDate(),
        entity.getModifiedBy().getId(),
        entity.getReason());
  }

  public NoteHistoryEntity toEntity(NoteHistory noteHistory) {
    if (noteHistory == null) {
      return null;
    }
    return NoteHistoryEntity.builder()
        .id(noteHistory.id())
        .note(NoteEntity.builder().id(noteHistory.noteId()).build())
        .previousValue(noteHistory.previousValue())
        .modificationDate(noteHistory.modificationDate())
        .modifiedBy(UserEntity.builder().id(noteHistory.modifiedById()).build())
        .reason(noteHistory.reason())
        .build();
  }
}
