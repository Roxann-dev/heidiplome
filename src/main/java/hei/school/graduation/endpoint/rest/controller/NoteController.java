package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.NoteCreateRequest;
import hei.school.graduation.dto.NoteUpdateRequest;
import hei.school.graduation.mapper.NoteHistoryMapper;
import hei.school.graduation.mapper.NoteMapper;
import hei.school.graduation.model.Note;
import hei.school.graduation.model.NoteHistory;
import hei.school.graduation.security.UserPrincipal;
import hei.school.graduation.service.NoteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NoteController {

  private final NoteService noteService;
  private final NoteMapper noteMapper;
  private final NoteHistoryMapper noteHistoryMapper;

  @GetMapping("/examens/{examenId}/notes")
  public List<Note> listByExam(@PathVariable UUID examenId) {
    return noteService.findByExam(examenId).stream().map(noteMapper::toDomain).toList();
  }

  @PostMapping("/examens/{examenId}/notes")
  public ResponseEntity<Note> create(
      @PathVariable UUID examenId, @Valid @RequestBody NoteCreateRequest request) {
    var created =
        noteService.saisir(examenId, request.studentId(), request.value(), currentUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(noteMapper.toDomain(created));
  }

  @PatchMapping("/notes/{noteId}")
  public Note update(@PathVariable UUID noteId, @Valid @RequestBody NoteUpdateRequest request) {
    var updated = noteService.update(noteId, request.value(), request.reason(), currentUserId());
    return noteMapper.toDomain(updated);
  }

  @GetMapping(value = "/notes/{noteId}/historique")
  public List<NoteHistory> history(@PathVariable UUID noteId) {
    return noteService.findHistory(noteId).stream().map(noteHistoryMapper::toDomain).toList();
  }

  private UUID currentUserId() {
    UserPrincipal principal =
        (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return principal.getId();
  }
}
