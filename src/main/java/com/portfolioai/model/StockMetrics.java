package com.portfolioai.model;

public class StockMetrics {

    public String ticker;

    // raw metrics
    public double totalReturn;     // e.g. 1.25 = +125%
    public double volatility;      // std dev of daily returns
    public double maxDrawdown;     // e.g. -0.55 = -55%

    // derived
    public double stabilityScore;  // higher = more stable
    public double finalScore;      // blended score used for ranking

    @Override
    public String toString() {
        return ticker +
                " return=" + totalReturn +
                " vol=" + volatility +
                " drawdown=" + maxDrawdown +
                " stability=" + stabilityScore +
                " score=" + finalScore;
    }
}
