package com.kalana.kanbanBoard.config;

import com.kalana.kanbanBoard.entity.Role;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Optional<User> existingAdmin = userRepository.findByUsername("admin");

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            admin.setEmail("admin@kanban.com");
            admin.setPasswordHash(passwordEncoder.encode("1234"));
            admin.setRole(Role.ADMIN);
            admin.setMustChangePassword(false);
            admin.setActive(true);
            userRepository.save(admin);
            log.info("Default admin user updated: username=admin, password=1234");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .email("admin@kanban.com")
                .passwordHash(passwordEncoder.encode("1234"))
                .role(Role.ADMIN)
                .mustChangePassword(false)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("Default admin user created: username=admin, password=1234");
    }
}
