package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.user.id = :userId")
    List<Project> findAllByMemberId(@Param("userId") Long userId);
}
