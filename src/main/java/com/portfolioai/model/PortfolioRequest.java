package com.portfolioai.model;

public class PortfolioRequest {

    private QuizAnswers answers;

    // optional request options (used by PortfolioService)
    private String optimizer;               // "mvo" or "risk_parity"
    private boolean useTemplatesIfNeeded = true;

    public QuizAnswers getAnswers() { return answers; }
    public void setAnswers(QuizAnswers answers) { this.answers = answers; }

    public String getOptimizer() { return optimizer; }
    public void setOptimizer(String optimizer) { this.optimizer = optimizer; }

    public boolean isUseTemplatesIfNeeded() { return useTemplatesIfNeeded; }
    public void setUseTemplatesIfNeeded(boolean useTemplatesIfNeeded) {
        this.useTemplatesIfNeeded = useTemplatesIfNeeded;
    }
}


