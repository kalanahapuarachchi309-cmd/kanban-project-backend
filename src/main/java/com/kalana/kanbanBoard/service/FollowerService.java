package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.FollowerDto;
import com.kalana.kanbanBoard.dto.FollowRequest;
import com.kalana.kanbanBoard.entity.Follower;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.exception.BadRequestException;
import com.kalana.kanbanBoard.exception.ConflictException;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.FollowerRepository;
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
public class FollowerService {

    private final FollowerRepository followerRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @Transactional
    public FollowerDto follow(FollowRequest request) {
        User currentUser = authUtil.getCurrentUser();

        if (currentUser.getId().equals(request.getFollowingUserId())) {
            throw new BadRequestException("Cannot follow yourself");
        }

        User targetUser = userRepository.findById(request.getFollowingUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getFollowingUserId()));

        if (followerRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUser.getId())) {
            throw new ConflictException("Already following this user");
        }

        Follower follower = Follower.builder()
                .follower(currentUser)
                .following(targetUser)
                .build();

        return Mapper.toFollowerDto(followerRepository.save(follower));
    }

    @Transactional
    public void unfollow(Long followId) {
        User currentUser = authUtil.getCurrentUser();
        Follower follower = followerRepository.findById(followId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found: " + followId));

        if (!follower.getFollower().getId().equals(currentUser.getId())) {
            throw new com.kalana.kanbanBoard.exception.AccessDeniedException("Cannot unfollow on behalf of others");
        }

        followerRepository.delete(follower);
    }

    public List<FollowerDto> getFollowing() {
        Long currentUserId = authUtil.getCurrentUserId();
        return followerRepository.findAllByFollowerId(currentUserId).stream()
                .map(Mapper::toFollowerDto)
                .collect(Collectors.toList());
    }

    public List<FollowerDto> getFollowers() {
        Long currentUserId = authUtil.getCurrentUserId();
        return followerRepository.findAllByFollowingId(currentUserId).stream()
                .map(Mapper::toFollowerDto)
                .collect(Collectors.toList());
    }
}
