package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.CreateUserRequest;
import com.kalana.kanbanBoard.dto.CreateUserByAdminResponse;
import com.kalana.kanbanBoard.dto.UserDto;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.exception.BadRequestException;
import com.kalana.kanbanBoard.exception.ConflictException;
import com.kalana.kanbanBoard.exception.EmailDeliveryException;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.CommentRepository;
import com.kalana.kanbanBoard.repository.FollowerRepository;
import com.kalana.kanbanBoard.repository.NotificationRepository;
import com.kalana.kanbanBoard.repository.PasswordResetTokenRepository;
import com.kalana.kanbanBoard.repository.ProjectMemberRepository;
import com.kalana.kanbanBoard.repository.UserRepository;
import com.kalana.kanbanBoard.repository.WorkItemRepository;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.Mapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final AuthService authService;
    private final EmailService emailService;
    private final ProjectMemberRepository projectMemberRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final FollowerRepository followerRepository;
    private final WorkItemRepository workItemRepository;

    @Transactional
    public UserDto registerUser(CreateUserRequest request) {
        // Public registration - ADMIN role not allowed
        if (request.getRole() == com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Cannot register as ADMIN. Please select another role.");
        }

        if (request.getTemporaryPassword() == null || request.getTemporaryPassword().isBlank()) {
            throw new BadRequestException("Temporary password is required");
        }

        if (request.getTemporaryPassword().length() < 6) {
            throw new BadRequestException("Temporary password must be at least 6 characters");
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
    public CreateUserByAdminResponse createUserByAdmin(CreateUserRequest request) {
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

        String generatedInitialPassword = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(generatedInitialPassword))
                .role(request.getRole())
                .mustChangePassword(true)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String emailWarning = null;

        if (user.getRole() != com.kalana.kanbanBoard.entity.Role.ADMIN) {
            String token = authService.createPasswordSetupToken(user);
            try {
                emailService.sendPasswordSetupEmail(user.getEmail(), user.getUsername(), token);
            } catch (EmailDeliveryException ex) {
                emailWarning = isSmtpAuthenticationFailure(ex)
                        ? "Authentication error: SMTP username/password is invalid. Check spring.mail.username, spring.mail.password, app.email.from"
                        : ex.getMessage();
                log.error(
                        "User created successfully but setup email failed. userId={}, username={}, email={}, reason={}. "
                                + "Check SMTP config: spring.mail.username, spring.mail.password, app.email.from",
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        ex.getMessage(),
                        ex);
            }
        }

        return CreateUserByAdminResponse.builder()
                .user(Mapper.toUserDto(user))
                .emailWarning(emailWarning)
                .build();
    }

    private boolean isSmtpAuthenticationFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                String normalized = msg.toLowerCase();
                if (normalized.contains("authentication failed")
                        || normalized.contains("535")
                        || normalized.contains("username and password not accepted")
                        || normalized.contains("auth")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
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

    @Transactional
    public void deleteUserByAdmin(Long userId) {
        User currentUser = authUtil.getCurrentUser();
        if (currentUser.getRole() != com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Only ADMIN users can delete users");
        }

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("You cannot delete your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getRole() == com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Admin users cannot be deleted");
        }

        boolean hasWorkItemDependencies = workItemRepository.existsByCreatedByIdOrAssignedToId(userId, userId);

        if (hasWorkItemDependencies) {
            throw new BadRequestException(
                    "Cannot delete user with existing work item references. Reassign related work items or deactivate the user instead.");
        }

        followerRepository.deleteAllByFollowerIdOrFollowingId(userId, userId);
        notificationRepository.deleteAllByUserId(userId);
        commentRepository.deleteAllByUserId(userId);
        projectMemberRepository.deleteAllByUserId(userId);
        passwordResetTokenRepository.deleteAllByUserId(userId);

        userRepository.delete(user);
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

    @Transactional
    public void resendPasswordSetupEmail(Long userId) {
        User currentUser = authUtil.getCurrentUser();
        if (currentUser.getRole() != com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Only ADMIN users can resend password setup links");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (targetUser.getRole() == com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Password setup resend is not allowed for ADMIN users");
        }

        if (!targetUser.isActive()) {
            throw new BadRequestException("Cannot send password setup email to an inactive user");
        }

        String token = authService.createPasswordSetupToken(targetUser);
        emailService.resendPasswordSetupEmail(targetUser.getEmail(), targetUser.getUsername(), token);
    }

    @Transactional
    public void resendPasswordSetupEmailForCurrentUser() {
        User currentUser = authUtil.getCurrentUser();

        if (currentUser.getRole() == com.kalana.kanbanBoard.entity.Role.ADMIN) {
            throw new BadRequestException("Password setup resend is not available for ADMIN users");
        }

        if (!currentUser.isActive()) {
            throw new BadRequestException("Cannot send password setup email to an inactive user");
        }

        if (!currentUser.isMustChangePassword()) {
            throw new BadRequestException("Password setup email is only available when password setup is required");
        }

        String token = authService.createPasswordSetupToken(currentUser);
        emailService.resendPasswordSetupEmail(currentUser.getEmail(), currentUser.getUsername(), token);
    }
}
