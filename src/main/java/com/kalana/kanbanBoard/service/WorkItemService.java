package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.*;
import com.kalana.kanbanBoard.entity.*;
import com.kalana.kanbanBoard.exception.AccessDeniedException;
import com.kalana.kanbanBoard.exception.BadRequestException;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.*;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.Mapper;
import com.kalana.kanbanBoard.util.WorkItemSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuthUtil authUtil;

    // ─────────────────────────── CREATE ───────────────────────────

    @Transactional
    public WorkItemDto createWorkItem(Long projectId, CreateWorkItemRequest request) {
        User currentUser = authUtil.getCurrentUser();
        projectService.ensureProjectMembership(projectId, currentUser.getId());
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        if (request.getType() == WorkItemType.BUG) {
            if (role != Role.QA_PM && role != Role.ADMIN && role != Role.CLIENT) {
                throw new AccessDeniedException("Only QA_PM, ADMIN or CLIENT can create BUG items");
            }
        } else {
            if (role == Role.DEVELOPER || role == Role.CLIENT) {
                throw new AccessDeniedException("Only QA_PM or ADMIN can create FEATURE items");
            }
        }

        User assignedUser = null;
        if (request.getAssignedTo() != null) {
            assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getAssignedTo()));
        }

        Project project = projectService.getProjectOrThrow(projectId);

        WorkItem workItem = WorkItem.builder()
                .project(project)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(WorkItemStatus.BUG_LIST)
                .createdBy(currentUser)
                .assignedTo(assignedUser)
                .dueAt(request.getDueAt())
                .build();

        workItem = workItemRepository.save(workItem);

        // Notify assignee if set
        if (assignedUser != null) {
            notifyAssignment(workItem, assignedUser);
        }

        return Mapper.toWorkItemDto(workItem);
    }

    @Transactional
    public WorkItemDto createBug(Long projectId, CreateBugRequest request) {
        User currentUser = authUtil.getCurrentUser();
        projectService.ensureProjectMembership(projectId, currentUser.getId());
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        if (role != Role.QA_PM && role != Role.ADMIN && role != Role.CLIENT) {
            throw new AccessDeniedException("Only QA_PM, ADMIN or CLIENT can create bugs");
        }

        User assignedUser = null;
        if (request.getAssignedTo() != null) {
            assignedUser = getAssignedDeveloperFromProject(projectId, request.getAssignedTo());
        }

        Project project = projectService.getProjectOrThrow(projectId);

        WorkItem workItem = WorkItem.builder()
                .project(project)
                .type(WorkItemType.BUG)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(WorkItemStatus.BUG_LIST)
                .createdBy(currentUser)
                .assignedTo(assignedUser)
                .dueAt(request.getDueAt())
                .build();

        workItem = workItemRepository.save(workItem);

        if (assignedUser != null) {
            notifyAssignment(workItem, assignedUser);
        }

        return Mapper.toWorkItemDto(workItem);
    }

    // ─────────────────────────── LIST / GET ───────────────────────────

    public List<WorkItemDto> getWorkItems(Long projectId, WorkItemFilterRequest filter) {
        User currentUser = authUtil.getCurrentUser();
        projectService.ensureProjectMembership(projectId, currentUser.getId());
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        Specification<WorkItem> spec = WorkItemSpecification.withFilter(projectId, filter);
        List<WorkItem> items = workItemRepository.findAll(spec);

        // CLIENT sees all project bugs plus published non-bug items.
        if (role == Role.CLIENT) {
            items = items.stream()
                    .filter(wi -> (wi.getType() == WorkItemType.BUG)
                            || (wi.getType() != WorkItemType.BUG && wi.getStatus() == WorkItemStatus.PUBLISHED))
                    .collect(Collectors.toList());
        }

        return items.stream().map(Mapper::toWorkItemDto).collect(Collectors.toList());
    }

    public List<WorkItemDto> getBugs(Long projectId) {
        User currentUser = authUtil.getCurrentUser();
        projectService.ensureProjectMembership(projectId, currentUser.getId());
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        WorkItemFilterRequest filter = new WorkItemFilterRequest();
        filter.setType(WorkItemType.BUG);
        filter.setSortBy("newest");
        if (role == Role.DEVELOPER) {
            // Developers should only see their own assigned bugs in bug list.
            filter.setAssignedTo(currentUser.getId());
        }

        List<WorkItem> items = new ArrayList<>(
                workItemRepository.findAll(WorkItemSpecification.withFilter(projectId, filter)));
        return items.stream().map(Mapper::toWorkItemDto).collect(Collectors.toList());
    }

    public List<WorkItemDto> getMyAssignedItems(Long projectId) {
        User currentUser = authUtil.getCurrentUser();

        if (currentUser.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException("Only DEVELOPER can view assigned work items");
        }

        List<WorkItem> items;
        if (projectId != null) {
            items = workItemRepository.findAllByAssignedToIdAndProjectId(currentUser.getId(), projectId);
        } else {
            items = workItemRepository.findAllByAssignedToId(currentUser.getId());
        }

        return items.stream().map(Mapper::toWorkItemDto).collect(Collectors.toList());
    }

    public WorkItemDto getWorkItem(Long workItemId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        Long userId = authUtil.getCurrentUserId();
        projectService.ensureProjectMembership(workItem.getProject().getId(), userId);
        return Mapper.toWorkItemDto(workItem);
    }

    @Transactional
    public void deleteWorkItem(Long workItemId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Role role = projectService.getMemberRole(workItem.getProject().getId(), currentUser.getId());

        if (role != Role.QA_PM && role != Role.ADMIN) {
            throw new AccessDeniedException("Only QA_PM or ADMIN can delete work items");
        }

        workItemRepository.delete(workItem);
    }

    // ─────────────────────────── UPDATE ───────────────────────────

    @Transactional
    public WorkItemDto updateWorkItem(Long workItemId, UpdateWorkItemRequest request) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Role role = projectService.getMemberRole(workItem.getProject().getId(), currentUser.getId());

        if (role == Role.CLIENT || role == Role.DEVELOPER) {
            throw new AccessDeniedException("Only ADMIN or QA_PM can update work items");
        }

        if (request.getTitle() != null)
            workItem.setTitle(request.getTitle());
        if (request.getDescription() != null)
            workItem.setDescription(request.getDescription());
        if (request.getPriority() != null)
            workItem.setPriority(request.getPriority());
        if (request.getDueAt() != null)
            workItem.setDueAt(request.getDueAt());

        return Mapper.toWorkItemDto(workItemRepository.save(workItem));
    }

    // ─────────────────────────── ASSIGN ───────────────────────────

    @Transactional
    public WorkItemDto assignWorkItem(Long workItemId, AssignWorkItemRequest request) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Role role = projectService.getMemberRole(workItem.getProject().getId(), currentUser.getId());

        if (role != Role.QA_PM && role != Role.ADMIN) {
            throw new AccessDeniedException("Only QA_PM or ADMIN can assign work items");
        }

        Long developerId = request.getDeveloperId();
        User assignedUser = getAssignedDeveloperFromProject(workItem.getProject().getId(), developerId);

        workItem.setAssignedTo(assignedUser);
        WorkItem savedWorkItem = workItemRepository.save(workItem);

        notifyAssignment(savedWorkItem, assignedUser);

        return Mapper.toWorkItemDto(savedWorkItem);
    }

    // ─────────────────────────── STATUS CHANGE ───────────────────────────

    @Transactional
    public WorkItemDto changeStatus(Long workItemId, StatusChangeRequest request) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Long projectId = workItem.getProject().getId();
        Role role = projectService.getMemberRole(projectId, currentUser.getId());
        WorkItemStatus fromStatus = workItem.getStatus();

        if (workItem.getStatus() == WorkItemStatus.IN_PROGRESS
                && request.getStatus() == WorkItemStatus.DONE
                && (role == Role.QA_PM || role == Role.ADMIN)) {
            workItem.setStatus(WorkItemStatus.DONE);

            if (workItem.getAssignedTo() != null) {
                notificationService.sendNotification(
                        workItem.getAssignedTo(),
                        NotificationType.STATUS_CHANGED,
                        "Status changed: " + workItem.getTitle(),
                        "Status updated to " + request.getStatus());
            }

            notifyQaPmMembersInProject(
                    workItem,
                    NotificationType.STATUS_CHANGED,
                    "Status changed: " + workItem.getTitle(),
                    "Status updated to " + request.getStatus() + " by " + currentUser.getUsername(),
                    currentUser.getId());

            return Mapper.toWorkItemDto(workItemRepository.save(workItem));
        }

        validateStatusTransition(workItem.getStatus(), request.getStatus(), role);

        workItem.setStatus(request.getStatus());
        boolean isDeveloperBugProgress = workItem.getType() == WorkItemType.BUG
                && role == Role.DEVELOPER
                && isDeveloperProgressTransition(fromStatus, request.getStatus());

        // Notify relevant parties
        if (workItem.getAssignedTo() != null) {
            notificationService.sendNotification(
                    workItem.getAssignedTo(),
                    NotificationType.STATUS_CHANGED,
                    "Status changed: " + workItem.getTitle(),
                    "Status updated to " + request.getStatus());
        }

        if (isDeveloperBugProgress) {
            notifyAdminAndQaPmForDeveloperBugProgress(workItem, currentUser, fromStatus, request.getStatus());
        } else {
            notifyQaPmMembersInProject(
                    workItem,
                    NotificationType.STATUS_CHANGED,
                    "Status changed: " + workItem.getTitle(),
                    "Status updated to " + request.getStatus() + " by " + currentUser.getUsername(),
                    currentUser.getId());
        }

        return Mapper.toWorkItemDto(workItemRepository.save(workItem));
    }

    // ─────────────────────────── PUBLISH ───────────────────────────

    @Transactional
    public WorkItemDto publishWorkItem(Long workItemId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Role role = projectService.getMemberRole(workItem.getProject().getId(), currentUser.getId());

        if (role != Role.QA_PM && role != Role.ADMIN) {
            throw new AccessDeniedException("Only QA_PM or ADMIN can publish work items");
        }
        if (workItem.getStatus() != WorkItemStatus.DONE) {
            throw new BadRequestException("Only DONE items can be published");
        }

        workItem.setStatus(WorkItemStatus.PUBLISHED);
        workItem.setPublishedAt(LocalDateTime.now());
        return Mapper.toWorkItemDto(workItemRepository.save(workItem));
    }

    // ─────────────────────────── CLIENT REVIEW ───────────────────────────

    @Transactional
    public WorkItemDto clientReview(Long workItemId, ClientReviewRequest request) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Long projectId = workItem.getProject().getId();
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        if (role != Role.CLIENT) {
            throw new AccessDeniedException("Only CLIENT can review published items");
        }
        if (workItem.getStatus() != WorkItemStatus.PUBLISHED) {
            throw new BadRequestException("Only PUBLISHED items can be reviewed");
        }

        workItem.setClientReviewStatus(request.getReviewStatus());

        if (request.getReviewStatus() == ClientReviewStatus.ACCEPTED) {
            workItem.setStatus(WorkItemStatus.ACCEPTED);
        } else if (request.getReviewStatus() == ClientReviewStatus.REJECTED) {
            workItem.setStatus(WorkItemStatus.BUG_LIST);
        }

        return Mapper.toWorkItemDto(workItemRepository.save(workItem));
    }

    // ─────────────────────────── HELPERS ───────────────────────────

    public WorkItem getWorkItemOrThrow(Long id) {
        return workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));
    }

    private void validateStatusTransition(WorkItemStatus from, WorkItemStatus to, Role role) {
        boolean valid = switch (from) {
            case BUG_LIST -> to == WorkItemStatus.IN_PROGRESS
                    && (role == Role.DEVELOPER || role == Role.QA_PM || role == Role.ADMIN);
            case IN_PROGRESS ->
                (to == WorkItemStatus.QA_FIX && role == Role.DEVELOPER)
                        || (to == WorkItemStatus.DONE && (role == Role.QA_PM || role == Role.ADMIN));
            case QA_FIX ->
                (to == WorkItemStatus.DONE || to == WorkItemStatus.BUG_LIST || to == WorkItemStatus.IN_PROGRESS)
                        && (role == Role.QA_PM || role == Role.ADMIN);
            case DONE -> (to == WorkItemStatus.PUBLISHED || to == WorkItemStatus.IN_PROGRESS)
                    && (role == Role.QA_PM || role == Role.ADMIN);
            case PUBLISHED -> (to == WorkItemStatus.ACCEPTED || to == WorkItemStatus.BUG_LIST)
                    && role == Role.CLIENT;
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition: " + from + " → " + to + " for role: " + role);
        }
    }

    private void notifyAssignment(WorkItem workItem, User assignee) {
        notificationService.sendNotification(
                assignee,
                NotificationType.ASSIGNMENT_CREATED,
                "New item assigned: " + workItem.getTitle(),
                "You have been assigned to: " + workItem.getTitle() +
                        (workItem.getDueAt() != null ? " | Due: " + workItem.getDueAt() : ""));

        notifyQaPmMembersInProject(
                workItem,
                NotificationType.ASSIGNMENT_CREATED,
                "Work item assigned: " + workItem.getTitle(),
                "Assigned to " + assignee.getUsername() +
                        (workItem.getDueAt() != null ? " | Due: " + workItem.getDueAt() : ""),
                assignee.getId());

        try {
            emailService.sendAssignmentEmail(
                    assignee.getEmail(),
                    assignee.getUsername(),
                    workItem.getProject().getName(),
                    workItem.getTitle(),
                    workItem.getDueAt() != null ? workItem.getDueAt().toString() : null,
                    workItem.getId());
        } catch (Exception ignored) {
        }
    }

    private void notifyQaPmMembersInProject(
            WorkItem workItem,
            NotificationType type,
            String title,
            String body,
            Long excludedUserId) {
        List<ProjectMember> qaPmMembers = projectMemberRepository.findAllByProjectIdAndRole(
                workItem.getProject().getId(),
                Role.QA_PM);

        Set<Long> notified = new HashSet<>();
        for (ProjectMember member : qaPmMembers) {
            User recipient = member.getUser();
            if (recipient == null || recipient.getId() == null) {
                continue;
            }
            if (excludedUserId != null && excludedUserId.equals(recipient.getId())) {
                continue;
            }
            if (!notified.add(recipient.getId())) {
                continue;
            }
            notificationService.sendNotification(recipient, type, title, body);
        }
    }

    private boolean isDeveloperProgressTransition(WorkItemStatus from, WorkItemStatus to) {
        return (from == WorkItemStatus.BUG_LIST && to == WorkItemStatus.IN_PROGRESS)
                || (from == WorkItemStatus.IN_PROGRESS && to == WorkItemStatus.QA_FIX);
    }

    private void notifyAdminAndQaPmForDeveloperBugProgress(
            WorkItem workItem,
            User developer,
            WorkItemStatus from,
            WorkItemStatus to) {
        List<ProjectMember> qaPmMembers = projectMemberRepository.findAllByProjectIdAndRole(
                workItem.getProject().getId(),
                Role.QA_PM);
        List<ProjectMember> adminMembers = projectMemberRepository.findAllByProjectIdAndRole(
                workItem.getProject().getId(),
                Role.ADMIN);

        Set<Long> notified = new HashSet<>();
        String title;
        String body;

        if (from == WorkItemStatus.IN_PROGRESS && to == WorkItemStatus.QA_FIX) {
            title = "Bug ready for QA review: BUG-" + workItem.getId();
            body = "Developer " + developer.getUsername() + " fixed BUG-" + workItem.getId()
                    + " (" + workItem.getTitle() + "). Bug has been fixed and is ready for QA review.";
        } else {
            title = "Bug moved: " + workItem.getTitle();
            body = "Developer " + developer.getUsername() + " moved bug '" + workItem.getTitle()
                    + "' from " + from + " to " + to;
        }

        for (ProjectMember member : qaPmMembers) {
            notifyProgressRecipient(member, developer.getId(), notified, title, body);
        }
        for (ProjectMember member : adminMembers) {
            notifyProgressRecipient(member, developer.getId(), notified, title, body);
        }
    }

    private void notifyProgressRecipient(
            ProjectMember member,
            Long excludedUserId,
            Set<Long> notified,
            String title,
            String body) {
        User recipient = member.getUser();
        if (recipient == null || recipient.getId() == null) {
            return;
        }
        if (excludedUserId != null && excludedUserId.equals(recipient.getId())) {
            return;
        }
        if (!notified.add(recipient.getId())) {
            return;
        }
        notificationService.sendNotification(recipient, NotificationType.STATUS_CHANGED, title, body);
    }

    private User getAssignedDeveloperFromProject(Long projectId, Long developerId) {
        User assignedUser = userRepository.findById(developerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + developerId));

        if (assignedUser.getRole() != Role.DEVELOPER) {
            throw new BadRequestException("Assigned user must have DEVELOPER role");
        }

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, developerId)
                .orElseThrow(() -> new BadRequestException(
                        "Developer is not assigned to this project. Admin must add the developer to the project first"));

        if (member.getRole() != Role.DEVELOPER) {
            throw new BadRequestException("Assigned user must have DEVELOPER role in this project");
        }

        return assignedUser;
    }
}
