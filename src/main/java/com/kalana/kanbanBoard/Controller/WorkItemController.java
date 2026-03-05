package com.kalana.kanbanBoard.Controller;

import com.kalana.kanbanBoard.dto.*;
import com.kalana.kanbanBoard.service.AttachmentService;
import com.kalana.kanbanBoard.service.WorkItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;
    private final AttachmentService attachmentService;

    // ────── Work Items ──────

    @PostMapping("/api/projects/{projectId}/work-items")
    public ResponseEntity<WorkItemDto> createWorkItem(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateWorkItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workItemService.createWorkItem(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/work-items")
    public ResponseEntity<List<WorkItemDto>> getWorkItems(
            @PathVariable Long projectId,
            WorkItemFilterRequest filter) {
        return ResponseEntity.ok(workItemService.getWorkItems(projectId, filter));
    }

    @GetMapping("/api/work-items/{id}")
    public ResponseEntity<WorkItemDto> getWorkItem(@PathVariable Long id) {
        return ResponseEntity.ok(workItemService.getWorkItem(id));
    }

    @PatchMapping("/api/work-items/{id}")
    public ResponseEntity<WorkItemDto> updateWorkItem(
            @PathVariable Long id,
            @RequestBody UpdateWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.updateWorkItem(id, request));
    }

    @PatchMapping("/api/work-items/{id}/assign")
    public ResponseEntity<WorkItemDto> assignWorkItem(
            @PathVariable Long id,
            @Valid @RequestBody AssignWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.assignWorkItem(id, request));
    }

    @PatchMapping("/api/work-items/{id}/status")
    public ResponseEntity<WorkItemDto> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(workItemService.changeStatus(id, request));
    }

    @PatchMapping("/api/work-items/{id}/publish")
    public ResponseEntity<WorkItemDto> publishWorkItem(@PathVariable Long id) {
        return ResponseEntity.ok(workItemService.publishWorkItem(id));
    }

    @PatchMapping("/api/work-items/{id}/client-review")
    public ResponseEntity<WorkItemDto> clientReview(
            @PathVariable Long id,
            @Valid @RequestBody ClientReviewRequest request) {
        return ResponseEntity.ok(workItemService.clientReview(id, request));
    }

    // ────── Attachments ──────

    @PostMapping(value = "/api/work-items/{id}/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.uploadAndSave(id, file));
    }

    @PostMapping("/api/work-items/{id}/attachments")
    public ResponseEntity<AttachmentDto> saveClientUploadedAttachment(
            @PathVariable Long id,
            @Valid @RequestBody AttachmentUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.saveFromClientUpload(id, request));
    }

    @GetMapping("/api/work-items/{id}/attachments")
    public ResponseEntity<List<AttachmentDto>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachments(id));
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
