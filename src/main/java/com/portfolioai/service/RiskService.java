package com.portfolioai.service;

import com.portfolioai.model.QuizAnswers;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RiskService {

    public static class RiskResult {
        public int score;
        public String tier;
        public Map<String, Double> constraints;

        public RiskResult(int score, String tier, Map<String, Double> constraints) {
            this.score = score;
            this.tier = tier;
            this.constraints = constraints;
        }
    }

    public RiskResult profile(QuizAnswers a) {
        int score = 0;

        score += switch (a.risk_tolerance) {
            case "low" -> 0; case "med" -> 1; case "high" -> 2; default -> 1;
        };
        score += switch (a.horizon) {
            case "<1y" -> 0; case "1-5y" -> 1; case "5y+" -> 2; default -> 1;
        };
        score += switch (a.goal) {
            case "income" -> 0; case "mix" -> 1; case "growth" -> 2; default -> 1;
        };
        score += switch (a.market_drop_reaction) {
            case "panic" -> 0; case "hold" -> 1; case "buy_more" -> 2; default -> 1;
        };

        String tier;
        Map<String, Double> constraints = new HashMap<>();

        // Risk tiers -> constraints (guardrails for optimizer)
        if (score <= 2) {
            tier = "conservative";
            constraints.put("eq_max", 0.50);
            constraints.put("bond_min", 0.40);
        } else if (score <= 5) {
            tier = "balanced";
            constraints.put("eq_min", 0.55);
            constraints.put("eq_max", 0.75);
            constraints.put("bond_min", 0.20);
        } else {
            tier = "aggressive";
            constraints.put("eq_min", 0.75);
            constraints.put("bond_max", 0.20);
        }

        return new RiskResult(score, tier, constraints);
    }
}

