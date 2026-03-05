package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findAllByWorkItemId(Long workItemId);
}
