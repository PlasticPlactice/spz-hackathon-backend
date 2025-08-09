package com.example.demo.Controller;

import com.example.demo.dto.AnalysisResponse;
import com.example.demo.Service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @PostMapping("/repositories/{owner}/{repo}/analysis")
    public Mono<AnalysisResponse> analyzeCommits(
            @PathVariable String owner,
            @PathVariable String repo) {

        // ★★★ この2行が重要です ★★★
        // TODO: Spring Security導入後、認証情報から実際のユーザーIDを取得する
        Long currentUserId = 1L; // ログ記録用の仮のユーザーID

        // 呼び出し時に currentUserId を追加します
        return analysisService.analyzeRepositoryCommits(owner, repo, currentUserId);
    }

    // デバッグ用のエンドポイント
    @GetMapping("/analysis/test-gemini")
    public Mono<String> testGemini() {
        return analysisService.testGeminiOnly();
    }
}