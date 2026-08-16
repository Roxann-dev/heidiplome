package hei.school.graduation.service;

import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public List<UserEntity> findAll(UserRole role) {
    return role == null ? userRepository.findAll() : userRepository.findByRole(role);
  }
}
