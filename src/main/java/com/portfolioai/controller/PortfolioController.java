package com.portfolioai.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.portfolioai.model.AiStockPortfolioResponse;
import com.portfolioai.model.PortfolioRequest;
import com.portfolioai.service.BacktestService;
import com.portfolioai.service.FreeAiStockPortfolioService;
import com.portfolioai.service.PortfolioService;

@RestController
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final BacktestService backtestService;
    private final FreeAiStockPortfolioService freeAiStockPortfolioService;

    public PortfolioController(
            PortfolioService portfolioService,
            BacktestService backtestService,
            FreeAiStockPortfolioService freeAiStockPortfolioService
    ) {
        this.portfolioService = portfolioService;
        this.backtestService = backtestService;
        this.freeAiStockPortfolioService = freeAiStockPortfolioService;
    }

    @PostMapping("/ai/stocks")
    public AiStockPortfolioResponse aiStocks(@RequestBody PortfolioRequest req) throws Exception {
        return freeAiStockPortfolioService.recommend(req.getAnswers());
    }
}
