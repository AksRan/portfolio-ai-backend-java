package com.portfolioai.service;

import com.portfolioai.model.BacktestResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BacktestService {

    private final MarketDataService marketDataService;

    public BacktestService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public BacktestResponse backtest(Map<String, Double> weights) throws Exception {

        List<String> portfolioAssets = List.of("VOO", "VXUS", "BND");
        var stats = marketDataService.loadReturns(portfolioAssets, 3);

        // portfolio daily returns
        double[] portRets = new double[stats.alignedReturns.get(0).length];
        for (int t = 0; t < portRets.length; t++) {
            double r = 0;
            for (int i = 0; i < portfolioAssets.size(); i++) {
                r += weights.get(portfolioAssets.get(i)) * stats.alignedReturns.get(i)[t];
            }
            portRets[t] = r;
        }

        // SPY stats for comparison
        var spyStats = marketDataService.loadReturns(List.of("SPY"), 3);
        double[] spyRets = spyStats.alignedReturns.get(0);

        return new BacktestResponse(
                cagr(portRets), vol(portRets),
                cagr(spyRets), vol(spyRets)
        );
    }

    private double cagr(double[] dailyRets) {
        double growth = 1.0;
        for (double r : dailyRets) growth *= (1 + r);

        double years = dailyRets.length / 252.0;
        return Math.pow(growth, 1.0 / years) - 1.0;
    }

    private double vol(double[] dailyRets) {
        double mean = 0;
        for (double r : dailyRets) mean += r;
        mean /= dailyRets.length;

        double var = 0;
        for (double r : dailyRets) var += (r - mean) * (r - mean);
        var /= (dailyRets.length - 1);

        return Math.sqrt(var) * Math.sqrt(252.0);
    }
}

