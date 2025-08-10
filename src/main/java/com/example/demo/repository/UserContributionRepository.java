package com.example.demo.repository;

import com.example.demo.entity.UserContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserContributionRepository extends JpaRepository<UserContribution, String> {
    List<UserContribution> findByUserIdAndCreatedAtAfter(Long userId, OffsetDateTime since);
    Optional<UserContribution> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}