package com.portfolioai.model;

public class PortfolioRequest {
    public QuizAnswers answers;
    public String optimizer; // "mvo" (default) or "risk_parity"
    public boolean useTemplatesIfNeeded = true;
}

