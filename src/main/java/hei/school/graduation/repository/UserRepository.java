package hei.school.graduation.repository;

import hei.school.graduation.entity.UserEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import hei.school.graduation.model.Enum.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByEmail(String email);

  List<UserEntity> findByRole(UserRole role);
}
