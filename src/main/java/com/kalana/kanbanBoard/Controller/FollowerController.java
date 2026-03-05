package com.kalana.kanbanBoard.Controller;

import com.kalana.kanbanBoard.dto.FollowerDto;
import com.kalana.kanbanBoard.dto.FollowRequest;
import com.kalana.kanbanBoard.service.FollowerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowerController {

    private final FollowerService followerService;

    @PostMapping
    public ResponseEntity<FollowerDto> follow(@Valid @RequestBody FollowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followerService.follow(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unfollow(@PathVariable Long id) {
        followerService.unfollow(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/following")
    public ResponseEntity<List<FollowerDto>> getFollowing() {
        return ResponseEntity.ok(followerService.getFollowing());
    }

    @GetMapping("/followers")
    public ResponseEntity<List<FollowerDto>> getFollowers() {
        return ResponseEntity.ok(followerService.getFollowers());
    }
}
