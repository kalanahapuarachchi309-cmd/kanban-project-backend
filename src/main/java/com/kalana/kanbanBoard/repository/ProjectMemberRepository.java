package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.ProjectMember;
import com.kalana.kanbanBoard.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectId(Long projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findAllByProjectIdAndRole(Long projectId, Role role);

    boolean existsByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
