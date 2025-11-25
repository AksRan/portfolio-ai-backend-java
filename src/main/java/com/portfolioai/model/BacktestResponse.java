package com.portfolioai.model;

public class BacktestResponse {
    public double portfolio_cagr;
    public double portfolio_vol;
    public double spy_cagr;
    public double spy_vol;

    public BacktestResponse(double portfolio_cagr, double portfolio_vol,
                            double spy_cagr, double spy_vol) {
        this.portfolio_cagr = portfolio_cagr;
        this.portfolio_vol = portfolio_vol;
        this.spy_cagr = spy_cagr;
        this.spy_vol = spy_vol;
    }
}
