package hei.school.graduation.service;

import hei.school.graduation.dto.UserCreateRequest;
import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.ConflictException;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public List<UserEntity> findAll(UserRole role) {
    return role == null ? userRepository.findAll() : userRepository.findByRole(role);
  }

  public UserEntity create(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("Email déjà utilisé : " + request.email());
    }
    if (userRepository.existsByReference(request.reference())) {
      throw new ConflictException("Référence déjà utilisée : " + request.reference());
    }

    UserEntity user =
        UserEntity.builder()
            .reference(request.reference())
            .lastName(request.lastName())
            .firstName(request.firstName())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(request.role())
            .build();

    return userRepository.save(user);
  }
}
