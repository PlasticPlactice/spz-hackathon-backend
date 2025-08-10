package com.example.demo.Controller;

import com.example.demo.Service.AnalysisService;
import com.example.demo.dto.AnalysisDto;
import com.example.demo.dto.AnalysisResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;
    private final UserRepository userRepository;

    public AnalysisController(AnalysisService analysisService, UserRepository userRepository) {
        this.analysisService = analysisService;
        this.userRepository = userRepository;
    }

    @PostMapping("/repositories/{owner}/{repo}/analysis")
    public Mono<AnalysisResponse> analyzeCommits(
            @PathVariable String owner,
            @PathVariable String repo) {
        // 認証ユーザーIDを取得
        Long currentUserId = getCurrentUserId();
        return analysisService.analyzeRepositoryCommits(owner, repo, currentUserId);
    }

    // デバッグ用のエンドポイント
    @GetMapping("/analysis/test-gemini")
    public Mono<String> testGemini() {
        return analysisService.testGeminiOnly();
    }

    @GetMapping("/analysis")
    public Mono<ResponseEntity<AnalysisDto>> getAnalysis(@RequestParam(defaultValue = "7") int duration) {
        User currentUser = getCurrentUser();
        return analysisService.analyzeUserActivity(currentUser, duration)
                .map(ResponseEntity::ok);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証情報がありません");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証ユーザーIDが不正です");
        }
    }

    private User getCurrentUser() {
        Long githubId = getCurrentUserId();
        return userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません"));
    }
}
