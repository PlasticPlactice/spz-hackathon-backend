package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "team_members")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // TeamMember(多) 対 User(1) の関係
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne // TeamMember(多) 対 Team(1) の関係
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    private OffsetDateTime deletedAt;
}