package com.kalana.kanbanBoard.Controller;

import com.kalana.kanbanBoard.dto.CreateUserRequest;
import com.kalana.kanbanBoard.dto.CreateUserByAdminResponse;
import com.kalana.kanbanBoard.dto.AdminResetPasswordResponse;
import com.kalana.kanbanBoard.dto.UserDto;
import com.kalana.kanbanBoard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(userService.getCurrentUserDto());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/developers")
    @PreAuthorize("hasAnyRole('ADMIN','QA_PM')")
    public ResponseEntity<List<UserDto>> getActiveDevelopers() {
        return ResponseEntity.ok(userService.getActiveDevelopers());
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateUserByAdminResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUserByAdmin(request));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PatchMapping("/{id:\\d+}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @PatchMapping("/{id:\\d+}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id:\\d+}/resend-password-setup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resendPasswordSetup(@PathVariable Long id) {
        userService.resendPasswordSetupEmail(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id:\\d+}/reset-temporary-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminResetPasswordResponse> resetTemporaryPassword(@PathVariable Long id) {
        return ResponseEntity.ok(userService.resetTemporaryPasswordByAdmin(id));
    }

    @PostMapping("/me/resend-password-setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resendPasswordSetupForCurrentUser() {
        userService.resendPasswordSetupEmailForCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
