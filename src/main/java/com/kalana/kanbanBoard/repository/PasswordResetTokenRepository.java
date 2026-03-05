package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findAllByUserIdAndUsedAtIsNull(Long userId);

    boolean existsByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
