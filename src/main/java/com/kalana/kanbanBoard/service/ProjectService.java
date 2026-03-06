package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.AddMemberRequest;
import com.kalana.kanbanBoard.dto.AddMembersRequest;
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
import java.util.Set;
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

        if (currentUser.getRole() == Role.QA_PM || currentUser.getRole() == Role.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(Mapper::toProjectDto)
                    .collect(Collectors.toList());
        }

        return projectRepository.findAllByMemberId(currentUser.getId()).stream()
                .map(Mapper::toProjectDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectMemberDto addMember(Long projectId, AddMemberRequest request) {
        User currentUser = authUtil.getCurrentUser();
        Project project = getProjectOrThrow(projectId);

        // ADMIN can add any role, QA_PM can add only developers
        Role managerRole = ensureProjectManagerRole(projectId, currentUser.getId());
        if (managerRole == Role.QA_PM && request.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException("QA_PM can add only DEVELOPER members to a project");
        }

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

    @Transactional
    public List<ProjectMemberDto> addMembers(Long projectId, AddMembersRequest request) {
        User currentUser = authUtil.getCurrentUser();
        Project project = getProjectOrThrow(projectId);

        // ADMIN can add any role, QA_PM can add only developers
        Role managerRole = ensureProjectManagerRole(projectId, currentUser.getId());
        if (managerRole == Role.QA_PM && request.getRole() != Role.DEVELOPER) {
            throw new AccessDeniedException("QA_PM can add only DEVELOPER members to a project");
        }

        Set<Long> uniqueUserIds = request.getUserIds().stream()
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<ProjectMember> createdMembers = uniqueUserIds.stream()
                .filter(userId -> !projectMemberRepository.existsByProjectIdAndUserId(projectId, userId))
                .map(userId -> {
                    User targetUser = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

                    return ProjectMember.builder()
                            .project(project)
                            .user(targetUser)
                            .role(request.getRole())
                            .build();
                })
                .collect(Collectors.toList());

        if (createdMembers.isEmpty()) {
            return List.of();
        }

        return projectMemberRepository.saveAll(createdMembers).stream()
                .map(Mapper::toProjectMemberDto)
                .collect(Collectors.toList());
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
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (currentUser.getRole() == Role.QA_PM || currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }

    public Role getMemberRole(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getRole)
                .orElseGet(() -> {
                    User currentUser = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    if (currentUser.getRole() == Role.QA_PM || currentUser.getRole() == Role.ADMIN) {
                        return currentUser.getRole();
                    }
                    throw new AccessDeniedException("You are not a member of this project");
                });
    }

    private Role ensureProjectManagerRole(Long projectId, Long userId) {
        Role role = getMemberRole(projectId, userId);
        if (role != Role.ADMIN && role != Role.QA_PM) {
            throw new AccessDeniedException("Only ADMIN or QA_PM can perform this action");
        }
        return role;
    }
}
