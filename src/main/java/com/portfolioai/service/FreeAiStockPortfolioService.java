package com.portfolioai.service;

import com.portfolioai.model.AiStockPortfolioResponse;
import com.portfolioai.model.QuizAnswers;
import com.portfolioai.model.StockMetrics;
import com.portfolioai.model.StockPick;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FreeAiStockPortfolioService {

    private final MetricsService metricsService;
    private final UniverseService universeService;

    public FreeAiStockPortfolioService(MetricsService metricsService, UniverseService universeService) {
        this.metricsService = metricsService;
        this.universeService = universeService;
    }

    public AiStockPortfolioResponse recommend(QuizAnswers answers) {

        // 1) Compute metrics for everything (sp500 + custom sectors)
        List<StockMetrics> all = metricsService.computeAllMetrics();

        // 2) Normalize so scores are comparable
        NormalizationUtil.normalizeReturns(all);
        NormalizationUtil.normalizeStability(all);

        // 3) Pick weights based on risk tolerance
        double wReturn, wStability;
        String tier;
        int riskScore;

        String rt = String.valueOf(answers.getRiskTolerance()).toLowerCase();
        switch (rt) {
            case "low" -> { tier = "conservative"; riskScore = 3; wReturn = 0.30; wStability = 0.70; }
            case "high" -> { tier = "aggressive";   riskScore = 8; wReturn = 0.70; wStability = 0.30; }
            default -> { tier = "balanced"; riskScore = 6; wReturn = 0.50; wStability = 0.50; }
        }

        // 4) Score each stock (higher is better for this user)
        for (StockMetrics m : all) {
            m.finalScore = wReturn * m.totalReturn + wStability * m.stabilityScore;
        }

        // 5) Pick top 6 (unique tickers)
        List<StockMetrics> top = all.stream()
                .sorted(Comparator.comparingDouble((StockMetrics m) -> m.finalScore).reversed())
                .collect(Collectors.toList());

        Map<String, String> tags = universeService.getTickerTags();

        List<StockPick> picks = new ArrayList<>();
        Set<String> used = new HashSet<>();

        for (StockMetrics m : top) {
            String t = (m.ticker == null) ? "" : m.ticker.trim().toUpperCase();
            if (t.isEmpty()) continue;
            if (!universeService.isAllowed(t)) continue;
            if (!used.add(t)) continue;

            StockPick p = new StockPick();
            p.ticker = t;
            p.tag = tags.getOrDefault(t, "sp500");
            p.weight = 1.0; // temp, we normalize next
            picks.add(p);

            if (picks.size() == 6) break;
        }

        if (picks.size() != 6) {
            throw new RuntimeException("Could not select 6 valid tickers from metrics universe.");
        }

        // 6) Convert scores -> weights (softmax-ish)
        // Use finalScore to create weights that sum to 1
        double max = top.stream().mapToDouble(m -> m.finalScore).max().orElse(0.0);

        double sum = 0.0;
        for (int i = 0; i < picks.size(); i++) {
            StockPick p = picks.get(i);
            StockMetrics m = top.stream().filter(x -> x.ticker.equalsIgnoreCase(p.ticker)).findFirst().orElse(null);
            double s = (m == null) ? 0.0 : m.finalScore;

            // exp(score - max) for stability
            p.weight = Math.exp(s - max);
            sum += p.weight;
        }
        for (StockPick p : picks) p.weight /= sum;

        // 7) Build response
        AiStockPortfolioResponse out = new AiStockPortfolioResponse();
        out.risk_score = riskScore;
        out.risk_tier = tier;
        out.picks = picks;
        out.explanation =
                "Free heuristic recommender: ranks stocks by blended score = " +
                wReturn + "*return + " + wStability + "*stability (based on past performance).";
        return out;
    }
}
