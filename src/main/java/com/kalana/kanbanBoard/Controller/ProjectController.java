package com.kalana.kanbanBoard.Controller;

import com.kalana.kanbanBoard.dto.AddMemberRequest;
import com.kalana.kanbanBoard.dto.AddMembersRequest;
import com.kalana.kanbanBoard.dto.CreateProjectRequest;
import com.kalana.kanbanBoard.dto.ProjectDto;
import com.kalana.kanbanBoard.dto.ProjectMemberDto;
import com.kalana.kanbanBoard.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMemberDto> addMember(@PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addMember(id, request));
    }

    @PostMapping("/{id}/members/bulk")
    public ResponseEntity<List<ProjectMemberDto>> addMembers(@PathVariable Long id,
            @Valid @RequestBody AddMembersRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addMembers(id, request));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberDto>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getMembers(id));
    }
}
