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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername().trim();

        User user = identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                        .orElseThrow(() -> {
                            log.warn("Login failed: user not found for identifier={}", identifier);
                            return new BadRequestException("Invalid username/email or password");
                        })
                : userRepository.findByUsername(identifier)
                        .orElseThrow(() -> {
                            log.warn("Login failed: user not found for identifier={}", identifier);
                            return new BadRequestException("Invalid username/email or password");
                        });

        if (!user.isActive()) {
            log.warn("Login failed: account deactivated. username={}, role={}", user.getUsername(), user.getRole());
            throw new BadRequestException("Account is deactivated");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            log.warn("Login failed: bad credentials. username={}, role={}, mustChangePassword={}",
                    user.getUsername(), user.getRole(), user.isMustChangePassword());
            if (user.isMustChangePassword()) {
                log.warn("Developer login blocked by password setup requirement. username={}", user.getUsername());
                throw new BadRequestException(
                        "Password setup required. Use 'Resend Password Setup Email' to create your password.");
            }
            throw new BadRequestException("Invalid username/email or password");
        }

        log.info("Login success. username={}, role={}", user.getUsername(), user.getRole());

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
