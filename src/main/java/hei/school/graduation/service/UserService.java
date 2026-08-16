package hei.school.graduation.service;

import hei.school.graduation.entity.UserEntity;
import hei.school.graduation.exception.NotFoundException;
import hei.school.graduation.model.Enum.UserRole;
import hei.school.graduation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public List<UserEntity> findAll(UserRole role) {
        return role == null ? userRepository.findAll() : userRepository.findByRole(role);
    }
}
