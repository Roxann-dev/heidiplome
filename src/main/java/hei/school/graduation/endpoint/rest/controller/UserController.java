package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.dto.UserCreateRequest;
import hei.school.graduation.mapper.UserMapper;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.User;
import hei.school.graduation.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserMapper userMapper;

  @GetMapping
  public List<User> list(@RequestParam(required = false) UserRole role) {
    return userService.findAll(role).stream().map(userMapper::toDomain).toList();
  }

  @PostMapping
  public ResponseEntity<User> create(@Valid @RequestBody UserCreateRequest request) {
    var created = userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDomain(created));
  }
}
