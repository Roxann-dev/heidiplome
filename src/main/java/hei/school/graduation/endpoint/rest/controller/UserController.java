package hei.school.graduation.endpoint.rest.controller;

import hei.school.graduation.mapper.UserMapper;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.model.User;
import hei.school.graduation.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
