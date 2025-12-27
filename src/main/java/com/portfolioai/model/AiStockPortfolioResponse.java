package com.portfolioai.model;

import java.util.List;

public class AiStockPortfolioResponse {
    public int risk_score;
    public String risk_tier; // "conservative" | "balanced" | "aggressive"
    public List<StockPick> picks; // ALWAYS 6
    public String explanation;
}
