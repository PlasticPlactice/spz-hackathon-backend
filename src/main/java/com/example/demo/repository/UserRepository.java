package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // findBy + カラム名 という命名規則でメソッドを定義するだけで、
    // Spring Data JPAが自動的に「githubId」でユーザーを検索するSQLを生成してくれます。
    Optional<User> findByGithubId(Long githubId);
}