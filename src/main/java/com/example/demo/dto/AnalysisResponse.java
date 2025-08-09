package com.example.demo.dto; // パッケージ名は適宜調整してください

// recordを使うと、フィールドを定義するだけで自動的にコンストラクタやgetterが作られます
public record AnalysisResponse(
        String repository,
        String analysisResult,
        String model
) {
}