package com.kalana.kanbanBoard.Controller;

import com.kalana.kanbanBoard.dto.AddCommentRequest;
import com.kalana.kanbanBoard.dto.CommentDto;
import com.kalana.kanbanBoard.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-items/{workItemId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long workItemId,
            @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(workItemId, request));
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long workItemId) {
        return ResponseEntity.ok(commentService.getComments(workItemId));
    }
}
