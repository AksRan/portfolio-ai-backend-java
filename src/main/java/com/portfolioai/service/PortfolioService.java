package com.portfolioai.service;

import com.portfolioai.model.PortfolioRequest;
import com.portfolioai.model.PortfolioResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PortfolioService {

    private final RiskService riskService;
    private final MarketDataService marketDataService;
    private final OptimizerService optimizerService;

    private final List<String> ASSETS = List.of("VOO", "VXUS", "BND");

    public PortfolioService(RiskService riskService,
                            MarketDataService marketDataService,
                            OptimizerService optimizerService) {
        this.riskService = riskService;
        this.marketDataService = marketDataService;
        this.optimizerService = optimizerService;
    }

    public PortfolioResponse generate(PortfolioRequest req) throws Exception {

        var rr = riskService.profile(req.answers);

        MarketDataService.ReturnStats stats = marketDataService.loadReturns(ASSETS, 3);

        Map<String, Double> weights;
        String optUsed;

        String optimizer = (req.optimizer == null) ? "mvo" : req.optimizer;

        try {
            if ("risk_parity".equalsIgnoreCase(optimizer)) {
                weights = optimizerService.riskParityOptimize(ASSETS, stats.cov, rr.constraints);
                optUsed = "risk_parity";
            } else {
                weights = optimizerService.mvoOptimize(ASSETS, stats.mu, stats.cov, rr.constraints);
                optUsed = "mvo";
            }
        } catch (Exception e) {
            if (!req.useTemplatesIfNeeded) throw e;
            weights = optimizerService.templatesForTier(rr.tier);
            optUsed = "templates";
        }

        String explanation = optimizerService.explanation(rr.tier, rr.constraints, weights);

        return new PortfolioResponse(rr.score, rr.tier, weights, explanation, optUsed);
    }
}

