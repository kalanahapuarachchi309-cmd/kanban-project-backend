package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.Follower;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowerRepository extends JpaRepository<Follower, Long> {

    List<Follower> findAllByFollowerId(Long followerId);

    List<Follower> findAllByFollowingId(Long followingId);

    Optional<Follower> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
