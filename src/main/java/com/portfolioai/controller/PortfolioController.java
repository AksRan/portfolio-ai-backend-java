package com.portfolioai.controller;

import com.portfolioai.model.BacktestResponse;
import com.portfolioai.model.PortfolioRequest;
import com.portfolioai.model.PortfolioResponse;
import com.portfolioai.service.BacktestService;
import com.portfolioai.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*") // required so Squarespace can call it
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final BacktestService backtestService;

    public PortfolioController(PortfolioService portfolioService,
                               BacktestService backtestService) {
        this.portfolioService = portfolioService;
        this.backtestService = backtestService;
    }

    @PostMapping("/portfolio")
    public PortfolioResponse portfolio(@RequestBody PortfolioRequest req) throws Exception {
        return portfolioService.generate(req);
    }

    @PostMapping("/backtest")
    public BacktestResponse backtest(@RequestBody PortfolioResponse resp) throws Exception {
        return backtestService.backtest(resp.assets);
    }
}

