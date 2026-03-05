package com.kalana.kanbanBoard.util;

import com.kalana.kanbanBoard.dto.*;
import com.kalana.kanbanBoard.entity.*;

import java.util.stream.Collectors;

public class Mapper {

    public static UserDto toUserDto(User user) {
        if (user == null)
            return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .mustChangePassword(user.isMustChangePassword())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static ProjectDto toProjectDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdBy(toUserDto(project.getCreatedBy()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static ProjectMemberDto toProjectMemberDto(ProjectMember pm) {
        return ProjectMemberDto.builder()
                .id(pm.getId())
                .projectId(pm.getProject().getId())
                .user(toUserDto(pm.getUser()))
                .role(pm.getRole())
                .createdAt(pm.getCreatedAt())
                .build();
    }

    public static AttachmentDto toAttachmentDto(Attachment a) {
        return AttachmentDto.builder()
                .id(a.getId())
                .workItemId(a.getWorkItem().getId())
                .url(a.getUrl())
                .publicId(a.getPublicId())
                .fileType(a.getFileType())
                .originalName(a.getOriginalName())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public static WorkItemDto toWorkItemDto(WorkItem wi) {
        return WorkItemDto.builder()
                .id(wi.getId())
                .projectId(wi.getProject().getId())
                .type(wi.getType())
                .title(wi.getTitle())
                .description(wi.getDescription())
                .status(wi.getStatus())
                .priority(wi.getPriority())
                .createdBy(toUserDto(wi.getCreatedBy()))
                .assignedTo(toUserDto(wi.getAssignedTo()))
                .dueAt(wi.getDueAt())
                .publishedAt(wi.getPublishedAt())
                .clientReviewStatus(wi.getClientReviewStatus())
                .createdAt(wi.getCreatedAt())
                .updatedAt(wi.getUpdatedAt())
                .attachments(wi.getAttachments().stream()
                        .map(Mapper::toAttachmentDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static CommentDto toCommentDto(Comment c) {
        return CommentDto.builder()
                .id(c.getId())
                .workItemId(c.getWorkItem().getId())
                .user(toUserDto(c.getUser()))
                .message(c.getMessage())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static NotificationDto toNotificationDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    public static FollowerDto toFollowerDto(Follower f) {
        return FollowerDto.builder()
                .id(f.getId())
                .follower(toUserDto(f.getFollower()))
                .following(toUserDto(f.getFollowing()))
                .createdAt(f.getCreatedAt())
                .build();
    }
}
