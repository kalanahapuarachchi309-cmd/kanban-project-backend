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
import java.util.List;
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

        // Only QA_PM and CLIENT can create work items
        if (role == Role.DEVELOPER) {
            throw new AccessDeniedException("Developers cannot create work items");
        }
        // CLIENT can only create BUGs
        if (role == Role.CLIENT && request.getType() != WorkItemType.BUG) {
            throw new AccessDeniedException("Clients can only create BUG items");
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

    // ─────────────────────────── LIST / GET ───────────────────────────

    public List<WorkItemDto> getWorkItems(Long projectId, WorkItemFilterRequest filter) {
        User currentUser = authUtil.getCurrentUser();
        projectService.ensureProjectMembership(projectId, currentUser.getId());
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        Specification<WorkItem> spec = WorkItemSpecification.withFilter(projectId, filter);
        List<WorkItem> items = workItemRepository.findAll(spec);

        // CLIENT sees only their project items, non-published items are hidden
        if (role == Role.CLIENT) {
            items = items.stream()
                    .filter(wi -> wi.getStatus() == WorkItemStatus.PUBLISHED
                            || wi.getCreatedBy().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return items.stream().map(Mapper::toWorkItemDto).collect(Collectors.toList());
    }

    public WorkItemDto getWorkItem(Long workItemId) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        Long userId = authUtil.getCurrentUserId();
        projectService.ensureProjectMembership(workItem.getProject().getId(), userId);
        return Mapper.toWorkItemDto(workItem);
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

        User assignedUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        workItem.setAssignedTo(assignedUser);
        workItem = workItemRepository.save(workItem);

        notifyAssignment(workItem, assignedUser);

        return Mapper.toWorkItemDto(workItem);
    }

    // ─────────────────────────── STATUS CHANGE ───────────────────────────

    @Transactional
    public WorkItemDto changeStatus(Long workItemId, StatusChangeRequest request) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        User currentUser = authUtil.getCurrentUser();
        Long projectId = workItem.getProject().getId();
        Role role = projectService.getMemberRole(projectId, currentUser.getId());

        validateStatusTransition(workItem.getStatus(), request.getStatus(), role);

        workItem.setStatus(request.getStatus());

        // Notify relevant parties
        if (workItem.getAssignedTo() != null) {
            notificationService.sendNotification(
                    workItem.getAssignedTo(),
                    NotificationType.STATUS_CHANGED,
                    "Status changed: " + workItem.getTitle(),
                    "Status updated to " + request.getStatus());
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
            case BUG_LIST -> to == WorkItemStatus.IN_PROGRESS && role == Role.DEVELOPER;
            case IN_PROGRESS -> to == WorkItemStatus.QA_FIX && role == Role.DEVELOPER;
            case QA_FIX -> (to == WorkItemStatus.DONE || to == WorkItemStatus.BUG_LIST)
                    && (role == Role.QA_PM || role == Role.ADMIN);
            case DONE -> to == WorkItemStatus.PUBLISHED && (role == Role.QA_PM || role == Role.ADMIN);
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
}
