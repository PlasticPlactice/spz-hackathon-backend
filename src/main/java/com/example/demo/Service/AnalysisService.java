package com.example.demo.Service;

import com.example.demo.dto.AnalysisResponse;
import com.example.demo.entity.AnalysisCache;
import com.example.demo.entity.AnalysisRequest;
import com.example.demo.repository.AnalysisCacheRepository;
import com.example.demo.repository.AnalysisRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AnalysisService {

    // --- 依存コンポーネント ---
    private final WebClient githubWebClient;
    private final WebClient geminiWebClient;
    private final AnalysisCacheRepository cacheRepository;
    private final AnalysisRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // --- コンストラクタ ---
    @Autowired
    public AnalysisService(
            WebClient.Builder webClientBuilder,
            @Value("${github.api.token}") String githubToken,
            AnalysisCacheRepository cacheRepository,
            AnalysisRequestRepository requestRepository,
            ObjectMapper objectMapper
    ) {
        this.cacheRepository = cacheRepository;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;

        // Gemini API用WebClient
        this.geminiWebClient = webClientBuilder.clone()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        // GitHub API用WebClient
        this.githubWebClient = webClientBuilder.baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .build();
    }

    // --- Gemini APIリクエスト用DTO ---
    private static class GeminiRequest {
        public final List<Content> contents;
        public GeminiRequest(String text) {
            this.contents = List.of(new Content(List.of(new Part(text))));
        }
    }
    private static class Content {
        public final List<Part> parts;
        public Content(List<Part> parts) { this.parts = parts; }
    }
    private static class Part {
        public final String text;
        public Part(String text) { this.text = text; }
    }

    // --- 主要なビジネスロジック ---

    /**
     * リポジトリ分析のメイン処理（キャッシュ対応）
     */
    public Mono<AnalysisResponse> analyzeRepositoryCommits(String owner, String repo, Long userId) {
        OffsetDateTime cacheThreshold = OffsetDateTime.now().minusHours(1);

        return Mono.justOrEmpty(cacheRepository.findFirstByRepoOwnerAndRepoNameAndAnalyzedAtAfterOrderByAnalyzedAtDesc(owner, repo, cacheThreshold))
                .flatMap(cache -> {
                    // --- Cache Hit ---
                    logRequest(userId, owner, repo, "cache_hit");
                    try {
                        AnalysisResponse response = objectMapper.readValue(cache.getResultJson(), AnalysisResponse.class);
                        return Mono.just(response);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("キャッシュの解析に失敗しました。", e));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // --- Cache Miss ---
                    return fetchFromGitHubAndAnalyze(owner, repo)
                            .doOnSuccess(response -> {
                                saveCache(owner, repo, response);
                                logRequest(userId, owner, repo, "success");
                            })
                            .doOnError(error -> {
                                logRequest(userId, owner, repo, "error");
                            });
                }));
    }

    /**
     * GitHubからのコミット取得とGeminiによる分析
     */
    private Mono<AnalysisResponse> fetchFromGitHubAndAnalyze(String owner, String repo) {
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/commits", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::extractCommitMessages)
                .flatMap(this::callGeminiApi)
                .map(analysisText -> new AnalysisResponse(
                        owner + "/" + repo,
                        analysisText,
                        "gemini-1.5-flash"
                ));
    }

    /**
     * Gemini APIを呼び出す共通メソッド
     */
    private Mono<String> executeGeminiCall(String prompt) {
        GeminiRequest requestBody = new GeminiRequest(prompt);

        return geminiWebClient.post()
                .uri("/v1beta/models/gemini-1.5-flash:generateContent")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("x-goog-api-key", this.geminiApiKey.trim()) // 認証エラーを解決したヘッダー方式
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("Gemini API Error Status: " + clientResponse.statusCode());
                                    System.err.println("Gemini API Error Body: " + errorBody);
                                    return Mono.error(new RuntimeException("Gemini APIがエラーを返しました。詳細はコンソールログを確認してください。"));
                                })
                )
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                    if (textNode.isMissingNode() || textNode.isNull()) {
                        String errorMessage = "Geminiからの応答形式が不正です: " + response.toString();
                        System.err.println(errorMessage);
                        return errorMessage;
                    }
                    return textNode.asText();
                })
                .doOnError(error -> System.err.println("Gemini API呼び出し中にエラー発生: " + error.getMessage()));
    }

    private Mono<String> callGeminiApi(List<String> commitMessages) {
        String prompt = "以下のGitコミットメッセージを分析し、開発の進捗や主な変更点を3つに要約してください。箇条書きで簡潔にお願いします。\n\n" + String.join("\n- ", commitMessages);
        return executeGeminiCall(prompt);
    }

    // --- デバッグ用メソッド ---

    public Mono<String> testGeminiOnly() {
        String prompt = "Hello, world!";
        return executeGeminiCall(prompt);
    }

    // --- データベース操作ヘルパー ---

    private void saveCache(String owner, String repo, AnalysisResponse response) {
        AnalysisCache newCache = new AnalysisCache();
        newCache.setRepoOwner(owner);
        newCache.setRepoName(repo);
        newCache.setAnalyzedAt(OffsetDateTime.now());
        try {
            newCache.setResultJson(objectMapper.writeValueAsString(response));
            cacheRepository.save(newCache);
        } catch (JsonProcessingException e) {
            System.err.println("キャッシュの保存に失敗しました: " + e.getMessage());
        }
    }

    private void logRequest(Long userId, String owner, String repo, String status) {
        AnalysisRequest newRequest = new AnalysisRequest();
        newRequest.setUserId(userId);
        newRequest.setRepoOwner(owner);
        newRequest.setRepoName(repo);
        newRequest.setStatus(status);
        newRequest.setRequestedAt(OffsetDateTime.now());
        requestRepository.save(newRequest);
    }

    private List<String> extractCommitMessages(JsonNode commitsArray) {
        return StreamSupport.stream(commitsArray.spliterator(), false)
                .map(commit -> commit.path("commit").path("message").asText())
                .collect(Collectors.toList());
    }
}