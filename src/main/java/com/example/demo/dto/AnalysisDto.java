package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AnalysisDto {
    private String startDate;
    private String endDate;
    private WeeklyTrend weeklyTrend;
    private DevelopmentTendency developmentTendency;

    @Getter
    @Builder
    public static class WeeklyTrend {
        private long totalCommits;
        private long featureCommits;
        private long fixCommits;
    }

    @Getter
    @Builder
    public static class DevelopmentTendency {
        private List<Map<String, Object>> workDistribution;
    }
}