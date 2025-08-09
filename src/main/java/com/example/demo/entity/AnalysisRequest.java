package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "analysis_requests")
public class AnalysisRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String repoOwner;

    private String repoName;

    private String status;

    @Column(nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();
}