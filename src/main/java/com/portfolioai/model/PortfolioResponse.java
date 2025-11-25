package com.portfolioai.model;

import java.util.Map;

public class PortfolioResponse {
    public int risk_score;
    public String risk_tier;
    public Map<String, Double> assets;
    public String explanation;
    public String optimizer_used;

    public PortfolioResponse(int risk_score, String risk_tier,
                             Map<String, Double> assets,
                             String explanation,
                             String optimizer_used) {
        this.risk_score = risk_score;
        this.risk_tier = risk_tier;
        this.assets = assets;
        this.explanation = explanation;
        this.optimizer_used = optimizer_used;
    }
}
