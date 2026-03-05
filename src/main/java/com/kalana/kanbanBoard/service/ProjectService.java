package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.AddMemberRequest;
import com.kalana.kanbanBoard.dto.CreateProjectRequest;
import com.kalana.kanbanBoard.dto.ProjectDto;
import com.kalana.kanbanBoard.dto.ProjectMemberDto;
import com.kalana.kanbanBoard.entity.Project;
import com.kalana.kanbanBoard.entity.ProjectMember;
import com.kalana.kanbanBoard.entity.Role;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.exception.AccessDeniedException;
import com.kalana.kanbanBoard.exception.ConflictException;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.ProjectMemberRepository;
import com.kalana.kanbanBoard.repository.ProjectRepository;
import com.kalana.kanbanBoard.repository.UserRepository;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        User currentUser = authUtil.getCurrentUser();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(currentUser)
                .build();
        project = projectRepository.save(project);

        // Creator becomes ADMIN of this project
        ProjectMember adminMember = ProjectMember.builder()
                .project(project)
                .user(currentUser)
                .role(Role.ADMIN)
                .build();
        projectMemberRepository.save(adminMember);

        return Mapper.toProjectDto(project);
    }

    public List<ProjectDto> getMyProjects() {
        User currentUser = authUtil.getCurrentUser();
        return projectRepository.findAllByMemberId(currentUser.getId()).stream()
                .map(Mapper::toProjectDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectMemberDto addMember(Long projectId, AddMemberRequest request) {
        User currentUser = authUtil.getCurrentUser();
        Project project = getProjectOrThrow(projectId);

        // Only ADMIN can add members
        ensureAdminRole(projectId, currentUser.getId());

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new ConflictException("User is already a member of this project");
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(targetUser)
                .role(request.getRole())
                .build();

        return Mapper.toProjectMemberDto(projectMemberRepository.save(member));
    }

    public List<ProjectMemberDto> getMembers(Long projectId) {
        ensureProjectMembership(projectId, authUtil.getCurrentUserId());
        return projectMemberRepository.findAllByProjectId(projectId).stream()
                .map(Mapper::toProjectMemberDto)
                .collect(Collectors.toList());
    }

    public Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    }

    public void ensureProjectMembership(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }

    public Role getMemberRole(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getRole)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));
    }

    private void ensureAdminRole(Long projectId, Long userId) {
        Role role = getMemberRole(projectId, userId);
        if (role != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}
