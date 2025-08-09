package com.example.demo.repository;

import com.example.demo.entity.AnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface AnalysisCacheRepository extends JpaRepository<AnalysisCache, Long> {

    Optional<AnalysisCache> findFirstByRepoOwnerAndRepoNameAndAnalyzedAtAfterOrderByAnalyzedAtDesc(
            String repoOwner, String repoName, OffsetDateTime analyzedAt);
}