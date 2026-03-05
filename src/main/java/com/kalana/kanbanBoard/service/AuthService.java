package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.AuthResponse;
import com.kalana.kanbanBoard.dto.ChangePasswordRequest;
import com.kalana.kanbanBoard.dto.LoginRequest;
import com.kalana.kanbanBoard.dto.ResendPasswordSetupRequest;
import com.kalana.kanbanBoard.dto.SetPasswordWithTokenRequest;
import com.kalana.kanbanBoard.entity.PasswordResetToken;
import com.kalana.kanbanBoard.entity.Role;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.exception.BadRequestException;
import com.kalana.kanbanBoard.repository.PasswordResetTokenRepository;
import com.kalana.kanbanBoard.repository.UserRepository;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = authUtil.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public String createPasswordSetupToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        var activeTokens = passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId());
        for (PasswordResetToken t : activeTokens) {
            t.setUsedAt(now);
        }
        passwordResetTokenRepository.saveAll(activeTokens);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(now.plusHours(24))
                .build();

        return passwordResetTokenRepository.save(token).getToken();
    }

    @Transactional
    public void setPasswordWithToken(SetPasswordWithTokenRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password setup link"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired password setup link");
        }

        User user = token.getUser();
        if (!user.isActive()) {
            throw new BadRequestException("User account is deactivated");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void resendPasswordSetupLink(ResendPasswordSetupRequest request) {
        String identifier = request.getUsernameOrEmail().trim();

        User user = identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new BadRequestException("User not found"))
                : userRepository.findByUsername(identifier)
                        .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Password setup resend is not allowed for ADMIN users");
        }

        if (!user.isActive()) {
            throw new BadRequestException("User account is deactivated");
        }

        if (!user.isMustChangePassword()) {
            throw new BadRequestException("Password setup is already completed for this user");
        }

        String token = createPasswordSetupToken(user);
        emailService.resendPasswordSetupEmail(user.getEmail(), user.getUsername(), token);
    }
}
