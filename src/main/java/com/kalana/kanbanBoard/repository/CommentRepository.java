package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByWorkItemIdOrderByCreatedAtAsc(Long workItemId);
}
