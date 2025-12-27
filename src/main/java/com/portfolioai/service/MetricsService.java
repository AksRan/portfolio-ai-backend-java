package com.portfolioai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.portfolioai.model.StockMetrics;

@Service
public class MetricsService {

    private final UniverseService universeService;
    private final MarketDataFreeService marketDataFreeService;

    public MetricsService(UniverseService universeService, MarketDataFreeService marketDataFreeService) {
        this.universeService = universeService;
        this.marketDataFreeService = marketDataFreeService;
    }

    /**
     * Computes all metrics for every ticker in UniverseService (SP500 + custom sectors).
     * Won't crash if a ticker fails (it will just get "bad" metrics).
     */
    public List<StockMetrics> computeAllMetrics() {
        List<StockMetrics> out = new ArrayList<>();

        for (String ticker : universeService.getAllowedTickers()) {
            try {
                if (ticker == null || ticker.isBlank()) continue;

                // ✅ This matches YOUR MarketDataFreeService
                List<Double> closes = marketDataFreeService.loadDailyClosesUS(ticker);

                StockMetrics m = compute(ticker, closes);
                out.add(m);

            } catch (Exception e) {
                StockMetrics m = new StockMetrics();
                m.ticker = ticker;
                m.totalReturn = 0;
                m.volatility = 1.0;
                m.maxDrawdown = -1.0;
                m.stabilityScore = 0;
                m.finalScore = 0;
                out.add(m);
            }
        }

        return out;
    }

    /**
     * Computes metrics from daily closes.
     * Closes must be chronological (oldest → newest).
     */
    public StockMetrics compute(String ticker, List<Double> closes) {
        StockMetrics m = new StockMetrics();
        m.ticker = ticker;

        if (closes == null || closes.size() < 50) {
            m.totalReturn = 0;
            m.volatility = 1.0;
            m.maxDrawdown = -1.0;
            m.stabilityScore = 0;
            m.finalScore = 0;
            return m;
        }

        double start = closes.get(0);
        double end = closes.get(closes.size() - 1);
        m.totalReturn = (end / start) - 1.0;

        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < closes.size(); i++) {
            double prev = closes.get(i - 1);
            double cur = closes.get(i);
            if (prev > 0 && cur > 0) {
                dailyReturns.add((cur / prev) - 1.0);
            }
        }

        m.volatility = stdDev(dailyReturns);
        m.maxDrawdown = computeMaxDrawdown(closes);

        double volPenalty = Math.max(m.volatility, 0.0001);
        double ddPenalty = Math.abs(m.maxDrawdown);
        m.stabilityScore = 1.0 / (volPenalty + ddPenalty + 0.01);

        m.finalScore = 0; // set later by FreeAiStockPortfolioService
        return m;
    }

    private double stdDev(List<Double> vals) {
        if (vals == null || vals.isEmpty()) return 0;

        double mean = vals.stream().mapToDouble(v -> v).average().orElse(0);
        double sumSq = 0;

        for (double v : vals) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / vals.size());
    }

    private double computeMaxDrawdown(List<Double> closes) {
        double peak = closes.get(0);
        double maxDd = 0;

        for (double c : closes) {
            if (c > peak) peak = c;
            double dd = (c / peak) - 1.0;
            if (dd < maxDd) maxDd = dd;
        }
        return maxDd;
    }
}
