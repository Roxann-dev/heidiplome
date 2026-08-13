package hei.school.graduation.mapper;

import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User toDomain(UserEntity entity) {
    if (entity == null) {
      return null;
    }
    return new User(
        entity.getId(),
        entity.getReference(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getEmail(),
        entity.getRole());
  }

  public UserEntity toEntity(User user) {
    if (user == null) {
      return null;
    }
    return UserEntity.builder()
        .id(user.id())
        .reference(user.reference())
        .firstName(user.firstName())
        .lastName(user.lastName())
        .email(user.email())
        .role(user.role())
        .build();
  }
}
