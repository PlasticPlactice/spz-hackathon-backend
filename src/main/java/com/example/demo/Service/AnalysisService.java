package com.example.demo.Service;

import com.example.demo.dto.AnalysisDto;
import com.example.demo.dto.AnalysisResponse;
import com.example.demo.entity.AnalysisCache;
import com.example.demo.entity.AnalysisRequest;
import com.example.demo.entity.User;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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

    public AnalysisService(WebClient.Builder webClientBuilder) {
        this.githubWebClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.geminiWebClient = webClientBuilder.clone()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.cacheRepository = null;
        this.requestRepository = null;
        this.objectMapper = new ObjectMapper();
    }

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
                .uri("/v1beta/models/gemini-2.0-flash-light:generateContent")
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
        String prompt = "以下のGitコミットメッセージを分析し、次の4項目について日本語で箇条書きで簡潔にまとめてください。各項目は配列ではなく、1つのまとまった文章として返してください。\n" +
                "1. 今週のハイライト\n" +
                "2. おすすめの改善点\n" +
                "3. トレンド分析\n" +
                "4. 今週の総評\n" +
                "\n【出力例】\n" +
                "今週のハイライト\n機能追加をリードした1週間でした。特にユーザー認証機能の実装で高い貢献度を示しています。\n\n" +
                "おすすめの改善点\n次はコードレビューにも挑戦してみましょう。チーム全体のコード品質向上に貢献できます。\n\n" +
                "トレンド分析\nバックエンド開発に集中していますが、フロントエンドスキルも伸ばすとフルスタック開発者として成長できます。\n\n" +
                "今週の総評\n全体的に活発な開発活動を行っています。特に機能追加において優れた成果を上げており、チームの主要な貢献者として活躍しています。継続的な成長が期待できます。\n";
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

    public Mono<AnalysisDto> analyzeUserActivity(User user, int durationDays) {
        // GitHub APIを呼び出す (ユーザーのトークンを使用)
        return fetchAllEvents(user)
                .takeWhile(event -> {
                    // イベントの日付が期間内か判定
                    String createdAt = event.path("created_at").asText("");
                    if (createdAt.isEmpty()) return true;
                    LocalDate eventDate = LocalDate.parse(createdAt.substring(0, 10));
                    return !eventDate.isBefore(LocalDate.now().minusDays(durationDays));
                })
                .collectList()
                .flatMap(events -> {
                    // PushEventのみ抽出し、コミット詳細取得
                    List<JsonNode> pushEvents = events.stream()
                            .filter(event -> "PushEvent".equals(event.path("type").asText()))
                            .toList();
                    return Flux.fromIterable(pushEvents)
                            .flatMap(this::getCommitDetails)
                            .collectList()
                            .map(commitDetails -> buildAnalysisDto(events, commitDetails, durationDays));
                });
    }

    private Flux<JsonNode> fetchAllEvents(User user) {
        // ページネーション対応（最大300件まで取得）
        return Flux.range(1, 3)
                .concatMap(page -> githubWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/users/{username}/events/public")
                                .queryParam("per_page", 100)
                                .queryParam("page", page)
                                .build(user.getUsername()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.getGithubAccessToken())
                        .retrieve()
                        .bodyToFlux(JsonNode.class)
                )
                .flatMap(Flux::fromIterable);
    }

    private Mono<JsonNode> getCommitDetails(JsonNode pushEvent) {
        // PushEventのpayload.commits配列からコミットメッセージやファイル情報を抽出
        // ここではpayload.commitsをそのまま返す
        JsonNode commits = pushEvent.path("payload").path("commits");
        if (commits.isArray()) {
            // 1つのPushEventに複数コミットが含まれる
            return Mono.just(commits);
        }
        return Mono.empty();
    }

    private AnalysisDto buildAnalysisDto(List<JsonNode> events, List<JsonNode> commitDetails, int durationDays) {
        // コミット集計
        long totalCommits = 0;
        long featureCommits = 0;
        long fixCommits = 0;
        int backend = 0, frontend = 0, other = 0;

        for (JsonNode commitsArray : commitDetails) {
            for (JsonNode commit : commitsArray) {
                totalCommits++;
                String msg = commit.path("message").asText("").toLowerCase();
                if (msg.contains("feature") || msg.contains("add") || msg.contains("implement")) featureCommits++;
                if (msg.contains("fix") || msg.contains("bug")) fixCommits++;
                // ファイルパスやリポジトリ名から分布を推定（例: ファイル名にfrontend, backendが含まれる場合）
                String url = commit.path("url").asText("");
                if (url.contains("frontend")) frontend++;
                else if (url.contains("backend")) backend++;
                else other++;
            }
        }
        int sum = backend + frontend + other;
        List<Map<String, Object>> workDist = List.of(
                Map.of("area", "Backend", "percentage", sum > 0 ? backend * 100 / sum : 0),
                Map.of("area", "Frontend", "percentage", sum > 0 ? frontend * 100 / sum : 0),
                Map.of("area", "Other", "percentage", sum > 0 ? other * 100 / sum : 0)
        );
        return AnalysisDto.builder()
                .startDate(LocalDate.now().minusDays(durationDays).toString())
                .endDate(LocalDate.now().toString())
                .weeklyTrend(AnalysisDto.WeeklyTrend.builder()
                        .totalCommits(totalCommits)
                        .featureCommits(featureCommits)
                        .fixCommits(fixCommits)
                        .build())
                .developmentTendency(AnalysisDto.DevelopmentTendency.builder()
                        .workDistribution(workDist)
                        .build())
                .build();
    }
}
