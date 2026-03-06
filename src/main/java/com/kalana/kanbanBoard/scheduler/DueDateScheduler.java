package com.kalana.kanbanBoard.scheduler;

import com.kalana.kanbanBoard.entity.NotificationType;
import com.kalana.kanbanBoard.entity.ProjectMember;
import com.kalana.kanbanBoard.entity.Role;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.entity.WorkItem;
import com.kalana.kanbanBoard.entity.WorkItemStatus;
import com.kalana.kanbanBoard.repository.ProjectMemberRepository;
import com.kalana.kanbanBoard.repository.WorkItemRepository;
import com.kalana.kanbanBoard.service.EmailService;
import com.kalana.kanbanBoard.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateScheduler {

    private final WorkItemRepository workItemRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private static final EnumSet<WorkItemStatus> COMPLETED_STATUSES = EnumSet.of(WorkItemStatus.DONE,
            WorkItemStatus.PUBLISHED, WorkItemStatus.ACCEPTED);

    /**
     * Runs every minute. Finds overdue and unnotified work items and sends
     * notifications.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void checkDueDates() {
        LocalDateTime now = LocalDateTime.now();
        List<WorkItem> overdueItems = workItemRepository
                .findByDueAtBeforeAndDueNotifiedFalseAndStatusNotInAndAssignedToIsNotNull(
                        now, COMPLETED_STATUSES);

        if (overdueItems.isEmpty())
            return;

        log.info("Due date scheduler: found {} overdue items", overdueItems.size());

        for (WorkItem item : overdueItems) {
            try {
                // WebSocket notification
                notificationService.sendNotification(
                        item.getAssignedTo(),
                        NotificationType.DUE_NOW,
                        "Due date reached: " + item.getTitle(),
                        "Work item \"" + item.getTitle() + "\" is now overdue. Due: " + item.getDueAt());

                List<ProjectMember> qaPmMembers = projectMemberRepository.findAllByProjectIdAndRole(
                        item.getProject().getId(),
                        Role.QA_PM);
                Set<Long> notified = new HashSet<>();
                if (item.getAssignedTo() != null && item.getAssignedTo().getId() != null) {
                    notified.add(item.getAssignedTo().getId());
                }

                for (ProjectMember member : qaPmMembers) {
                    User recipient = member.getUser();
                    if (recipient == null || recipient.getId() == null) {
                        continue;
                    }
                    if (!notified.add(recipient.getId())) {
                        continue;
                    }
                    notificationService.sendNotification(
                            recipient,
                            NotificationType.DUE_NOW,
                            "Due date reached: " + item.getTitle(),
                            "Work item \"" + item.getTitle() + "\" is overdue and needs QA/PM attention.");
                }

                // Email notification
                emailService.sendDueDateEmail(
                        item.getAssignedTo().getEmail(),
                        item.getAssignedTo().getUsername(),
                        item.getProject().getName(),
                        item.getTitle(),
                        item.getId());

                item.setDueNotified(true);
                workItemRepository.save(item);

            } catch (Exception e) {
                log.error("Failed to send due-date notification for item {}: {}",
                        item.getId(), e.getMessage());
            }
        }
    }
}
