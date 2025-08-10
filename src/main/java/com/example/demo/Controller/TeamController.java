package com.example.demo.Controller;

import com.example.demo.entity.Team;
import com.example.demo.repository.TeamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

/**
 * TeamエンティティのCRUD操作を行うためのREST APIコントローラーです。
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamRepository teamRepository;

    public TeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // CREATE: 新しいチームを作成します。
    @PostMapping
    public Team createTeam(@RequestBody Team team) {
        // 現在のタイムスタンプを設定し、保存します。
        team.setCreatedAt(java.time.OffsetDateTime.now());
        team.setUpdatedAt(java.time.OffsetDateTime.now());
        return teamRepository.save(team);
    }

    // READ: 全てのチームを取得します。
    @GetMapping
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    // READ: IDを指定してチームを取得します。
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Optional<Team> team = teamRepository.findById(id);
        return team.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // UPDATE: 既存のチームを更新します。
    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(@PathVariable Long id, @RequestBody Team teamDetails) {
        Optional<Team> optionalTeam = teamRepository.findById(id);
        if (optionalTeam.isPresent()) {
            Team team = optionalTeam.get();
            team.setName(teamDetails.getName());
            team.setUpdatedAt(java.time.OffsetDateTime.now());
            Team updatedTeam = teamRepository.save(team);
            return ResponseEntity.ok(updatedTeam);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE: IDを指定してチームを削除します。
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        if (teamRepository.existsById(id)) {
            teamRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
