package com.portfolioai.service;

import java.util.List;

import com.portfolioai.model.StockMetrics;

public class NormalizationUtil {

    public static void normalizeReturns(List<StockMetrics> metrics) {
        double min = metrics.stream().mapToDouble(m -> m.totalReturn).min().orElse(0);
        double max = metrics.stream().mapToDouble(m -> m.totalReturn).max().orElse(1);

        for (StockMetrics m : metrics) {
            m.totalReturn = normalize(m.totalReturn, min, max);
        }
    }

    public static void normalizeStability(List<StockMetrics> metrics) {
        double min = metrics.stream().mapToDouble(m -> m.stabilityScore).min().orElse(0);
        double max = metrics.stream().mapToDouble(m -> m.stabilityScore).max().orElse(1);

        for (StockMetrics m : metrics) {
            m.stabilityScore = normalize(m.stabilityScore, min, max);
        }
    }

    private static double normalize(double v, double min, double max) {
        if (max - min == 0) return 0.5;
        return (v - min) / (max - min);
    }
}
