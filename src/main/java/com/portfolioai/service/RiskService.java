package com.portfolioai.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.portfolioai.model.QuizAnswers;

@Service
public class RiskService {

    public static class RiskResult {
        public int score;
        public String tier;
        public Map<String, Double> constraints;
    }

    public RiskResult profile(QuizAnswers a) {
        int score = 0;

        score += switch (a.getRiskTolerance()) {
            case "low" -> 0;
            case "med" -> 1;
            case "high" -> 2;
            default -> 1;
        };

        score += switch (a.getHorizon()) {
            case "1y" -> 0;
            case "1-5y" -> 1;
            case "5y+" -> 2;
            default -> 1;
        };

        score += switch (a.getGoal()) {
            case "income" -> 0;
            case "mix" -> 1;
            case "growth" -> 2;
            default -> 1;
        };

        score += switch (a.getMarketDropReaction()) {
            case "panic" -> 0;
            case "hold" -> 1;
            case "buy_more" -> 2;
            default -> 1;
        };

        String tier;
        Map<String, Double> constraints = new HashMap<>();

        if (score <= 2) {
            tier = "conservative";
            constraints.put("stocks", 0.4);
        } else if (score <= 5) {
            tier = "balanced";
            constraints.put("stocks", 0.7);
        } else {
            tier = "aggressive";
            constraints.put("stocks", 0.9);
        }

        RiskResult result = new RiskResult();
        result.score = score;
        result.tier = tier;
        result.constraints = constraints;
        return result;
    }
}
