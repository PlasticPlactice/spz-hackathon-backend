package com.example.demo.repository;

import com.example.demo.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    // 今後、「特定のユーザーが所有するチーム一覧」などの機能が必要になれば、
    // ここに List<Team> findByOwner(User owner); のようなメソッドを追加します。
}