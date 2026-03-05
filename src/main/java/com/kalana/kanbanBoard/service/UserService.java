package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.CreateUserRequest;
import com.kalana.kanbanBoard.dto.UserDto;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.exception.BadRequestException;
import com.kalana.kanbanBoard.exception.ConflictException;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.UserRepository;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final AuthService authService;
    private final EmailService emailService;

    @Transactional
    public UserDto registerUser(CreateUserRequest request) {
        // Public registration - ADMIN role not allowed
        if (request.getRole() == com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Cannot register as ADMIN. Please select another role.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(request.getRole())
                .mustChangePassword(true)
                .isActive(true)
                .build();

        return Mapper.toUserDto(userRepository.save(user));
    }

    @Transactional
    public UserDto createUserByAdmin(CreateUserRequest request) {
        // Only ADMIN can create users with any role including other ADMINs
        User currentUser = authUtil.getCurrentUser();
        if (currentUser.getRole() != com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Only ADMIN users can create new users");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(request.getRole())
                .mustChangePassword(true)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String token = authService.createPasswordSetupToken(user);
        emailService.sendPasswordSetupEmail(
                user.getEmail(),
                user.getUsername(),
                request.getTemporaryPassword(),
                token);

        return Mapper.toUserDto(user);
    }

    @Transactional
    public UserDto deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(false);
        return Mapper.toUserDto(userRepository.save(user));
    }

    @Transactional
    public UserDto activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(true);
        return Mapper.toUserDto(userRepository.save(user));
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(Mapper::toUserDto)
                .collect(Collectors.toList());
    }

    public UserDto getUser(Long userId) {
        return Mapper.toUserDto(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId)));
    }

    public UserDto getCurrentUserDto() {
        return Mapper.toUserDto(authUtil.getCurrentUser());
    }
}
