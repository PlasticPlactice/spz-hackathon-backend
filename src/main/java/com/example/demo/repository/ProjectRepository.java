package com.example.demo.repository;

import com.example.demo.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    // チームIDでプロジェクト一覧を検索するメソッド
    List<Project> findByTeamId(Long teamId);
}