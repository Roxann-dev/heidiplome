package hei.school.graduation.mapper;

import hei.school.graduation.entity.ExamEntity;
import hei.school.graduation.entity.NoteEntity;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Note;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

  public Note toDomain(NoteEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Note(
        entity.getId(),
        entity.getExam().getId(),
        entity.getStudent().getId(),
        entity.getEnteredBy() != null ? entity.getEnteredBy().getId() : null,
        entity.getValue(),
        entity.getEntryDate());
  }

  public NoteEntity toEntity(Note note) {
    if (note == null) {
      return null;
    }
    return NoteEntity.builder()
        .id(note.id())
        .exam(ExamEntity.builder().id(note.examId()).build())
        .student(UserEntity.builder().id(note.studentId()).build())
        .enteredBy(
            note.enteredById() != null ? UserEntity.builder().id(note.enteredById()).build() : null)
        .value(note.value())
        .entryDate(note.entryDate())
        .build();
  }
}
