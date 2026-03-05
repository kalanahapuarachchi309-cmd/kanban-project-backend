package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.AddCommentRequest;
import com.kalana.kanbanBoard.dto.CommentDto;
import com.kalana.kanbanBoard.entity.Comment;
import com.kalana.kanbanBoard.entity.NotificationType;
import com.kalana.kanbanBoard.entity.WorkItem;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.repository.CommentRepository;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final WorkItemService workItemService;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final AuthUtil authUtil;

    @Transactional
    public CommentDto addComment(Long workItemId, AddCommentRequest request) {
        User currentUser = authUtil.getCurrentUser();
        WorkItem workItem = workItemService.getWorkItemOrThrow(workItemId);
        projectService.ensureProjectMembership(workItem.getProject().getId(), currentUser.getId());

        Comment comment = Comment.builder()
                .workItem(workItem)
                .user(currentUser)
                .message(request.getMessage())
                .build();

        comment = commentRepository.save(comment);

        // Notify assignee about new comment (if different from commenter)
        if (workItem.getAssignedTo() != null
                && !workItem.getAssignedTo().getId().equals(currentUser.getId())) {
            notificationService.sendNotification(
                    workItem.getAssignedTo(),
                    NotificationType.COMMENT_ADDED,
                    "New comment on: " + workItem.getTitle(),
                    currentUser.getUsername() + " commented: " + request.getMessage());
        }

        return Mapper.toCommentDto(comment);
    }

    public List<CommentDto> getComments(Long workItemId) {
        User currentUser = authUtil.getCurrentUser();
        WorkItem workItem = workItemService.getWorkItemOrThrow(workItemId);
        projectService.ensureProjectMembership(workItem.getProject().getId(), currentUser.getId());

        return commentRepository.findAllByWorkItemIdOrderByCreatedAtAsc(workItemId).stream()
                .map(Mapper::toCommentDto)
                .collect(Collectors.toList());
    }
}
