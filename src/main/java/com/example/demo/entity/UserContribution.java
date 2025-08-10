package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_contributions")
@Getter
@Setter
public class UserContribution {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}