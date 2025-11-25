package com.portfolioai.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OptimizerService {

    // Idea 3 v1 templates (fallback)
    public Map<String, Double> templatesForTier(String tier) {
        return switch (tier) {
            case "conservative" -> Map.of("VOO", 0.35, "VXUS", 0.15, "BND", 0.45);
            case "balanced" -> Map.of("VOO", 0.50, "VXUS", 0.15, "BND", 0.35);
            default -> Map.of("VOO", 0.70, "VXUS", 0.20, "BND", 0.10); // aggressive
        };
    }

    // Idea 3 v2 constrained Mean-Variance Optimization
    public Map<String, Double> mvoOptimize(List<String> assets,
                                          double[] mu,
                                          double[][] cov,
                                          Map<String, Double> constraints) {

        int n = assets.size();
        double[] w = new double[n];
        Arrays.fill(w, 1.0 / n);

        double lr = 0.01;

        for (int iter = 0; iter < 4000; iter++) {
            double[] grad = new double[n];

            for (int i = 0; i < n; i++) {
                double riskPart = 0;
                for (int j = 0; j < n; j++) riskPart += cov[i][j] * w[j];
                grad[i] = 2 * riskPart - mu[i];
            }

            for (int i = 0; i < n; i++) {
                w[i] -= lr * grad[i];
                w[i] = Math.max(0, Math.min(1, w[i]));
            }
            normalize(w);

            // equities = VOO + VXUS, bonds = BND (assumes ordering)
            double eqWeight = w[0] + w[1];
            double bondWeight = w[2];

            if (constraints.containsKey("eq_min") && eqWeight < constraints.get("eq_min")) {
                double deficit = constraints.get("eq_min") - eqWeight;
                w[0] += deficit / 2; w[1] += deficit / 2; w[2] -= deficit;
            }
            if (constraints.containsKey("eq_max") && eqWeight > constraints.get("eq_max")) {
                double excess = eqWeight - constraints.get("eq_max");
                w[0] -= excess / 2; w[1] -= excess / 2; w[2] += excess;
            }
            if (constraints.containsKey("bond_min") && bondWeight < constraints.get("bond_min")) {
                double deficit = constraints.get("bond_min") - bondWeight;
                w[2] += deficit; w[0] -= deficit / 2; w[1] -= deficit / 2;
            }
            if (constraints.containsKey("bond_max") && bondWeight > constraints.get("bond_max")) {
                double excess = bondWeight - constraints.get("bond_max");
                w[2] -= excess; w[0] += excess / 2; w[1] += excess / 2;
            }

            for (int i = 0; i < n; i++) w[i] = Math.max(0, Math.min(1, w[i]));
            normalize(w);
        }

        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(assets.get(i), w[i]);
        return out;
    }

    // Idea 3 v3 Risk Parity (equal risk contribution approx)
    public Map<String, Double> riskParityOptimize(List<String> assets,
                                                  double[][] cov,
                                                  Map<String, Double> constraints) {
        int n = assets.size();
        double[] w = new double[n];
        Arrays.fill(w, 1.0 / n);

        double lr = 0.01;

        for (int iter = 0; iter < 5000; iter++) {
            double[] mrc = marginalRisk(cov, w); // Σw
            double portVar = dot(w, mrc);
            double[] rc = new double[n];

            for (int i = 0; i < n; i++) rc[i] = w[i] * mrc[i] / Math.sqrt(portVar);

            double target = Arrays.stream(rc).average().orElse(0);
            double[] grad = new double[n];
            for (int i = 0; i < n; i++) grad[i] = rc[i] - target;

            for (int i = 0; i < n; i++) {
                w[i] -= lr * grad[i];
                w[i] = Math.max(0, Math.min(1, w[i]));
            }
            normalize(w);

            // apply same equity/bond constraints
            double eqWeight = w[0] + w[1];
            double bondWeight = w[2];

            if (constraints.containsKey("eq_min") && eqWeight < constraints.get("eq_min")) {
                double deficit = constraints.get("eq_min") - eqWeight;
                w[0] += deficit / 2; w[1] += deficit / 2; w[2] -= deficit;
            }
            if (constraints.containsKey("eq_max") && eqWeight > constraints.get("eq_max")) {
                double excess = eqWeight - constraints.get("eq_max");
                w[0] -= excess / 2; w[1] -= excess / 2; w[2] += excess;
            }
            if (constraints.containsKey("bond_min") && bondWeight < constraints.get("bond_min")) {
                double deficit = constraints.get("bond_min") - bondWeight;
                w[2] += deficit; w[0] -= deficit / 2; w[1] -= deficit / 2;
            }
            if (constraints.containsKey("bond_max") && bondWeight > constraints.get("bond_max")) {
                double excess = bondWeight - constraints.get("bond_max");
                w[2] -= excess; w[0] += excess / 2; w[1] += excess / 2;
            }

            for (int i = 0; i < n; i++) w[i] = Math.max(0, Math.min(1, w[i]));
            normalize(w);
        }

        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(assets.get(i), w[i]);
        return out;
    }

    public String explanation(String tier, Map<String, Double> constraints, Map<String, Double> weights) {
        double eq = weights.get("VOO") + weights.get("VXUS");
        double bond = weights.get("BND");

        return "Risk tier: " + tier.toUpperCase() + "\n"
                + "Constraints applied: " + constraints + "\n\n"
                + String.format("Portfolio leans %.0f%% equities and %.0f%% bonds "
                + "to match your risk appetite and horizon. Equities support growth; "
                + "bonds reduce volatility.", eq * 100, bond * 100);
    }

    // ---- helpers ----
    private void normalize(double[] w) {
        double sum = 0;
        for (double v : w) sum += v;
        if (sum == 0) return;
        for (int i = 0; i < w.length; i++) w[i] /= sum;
    }

    private double[] marginalRisk(double[][] cov, double[] w) {
        int n = w.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            for (int j = 0; j < n; j++) s += cov[i][j] * w[j];
            out[i] = s;
        }
        return out;
    }

    private double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}

