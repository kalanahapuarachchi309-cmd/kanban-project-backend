package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.WorkItem;
import com.kalana.kanbanBoard.entity.WorkItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long>, JpaSpecificationExecutor<WorkItem> {

    List<WorkItem> findAllByProjectId(Long projectId);

    List<WorkItem> findAllByAssignedToId(Long userId);

    List<WorkItem> findAllByAssignedToIdAndProjectId(Long userId, Long projectId);

    List<WorkItem> findByDueAtBeforeAndDueNotifiedFalseAndStatusNotInAndAssignedToIsNotNull(
            LocalDateTime now,
            java.util.Collection<WorkItemStatus> excludedStatuses);

    boolean existsByCreatedByIdOrAssignedToId(Long createdById, Long assignedToId);
}
