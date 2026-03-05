package com.kalana.kanbanBoard.Controller;

import com.kalana.kanbanBoard.dto.AuthResponse;
import com.kalana.kanbanBoard.dto.ChangePasswordRequest;
import com.kalana.kanbanBoard.dto.LoginRequest;
import com.kalana.kanbanBoard.dto.ResendPasswordSetupRequest;
import com.kalana.kanbanBoard.dto.SetPasswordWithTokenRequest;
import com.kalana.kanbanBoard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless; client discards token
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-setup")
    public ResponseEntity<Void> setPasswordWithToken(@Valid @RequestBody SetPasswordWithTokenRequest request) {
        authService.setPasswordWithToken(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-setup/resend")
    public ResponseEntity<Void> resendPasswordSetupLink(@Valid @RequestBody ResendPasswordSetupRequest request) {
        authService.resendPasswordSetupLink(request);
        return ResponseEntity.noContent().build();
    }
}
