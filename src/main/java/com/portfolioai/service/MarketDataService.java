package com.portfolioai.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

@Service
public class MarketDataService {

    private static final Map<String, String> STOOQ = Map.of(
            "VOO", "voo.us",
            "VXUS", "vxus.us",
            "BND", "bnd.us",
            "SPY", "spy.us"
    );

    public static class ReturnStats {
        public double[] mu;
        public double[][] cov;
        public List<double[]> alignedReturns;

        public ReturnStats(double[] mu, double[][] cov, List<double[]> alignedReturns) {
            this.mu = mu;
            this.cov = cov;
            this.alignedReturns = alignedReturns;
        }
    }

    public ReturnStats loadReturns(List<String> assets, int yearsBack) throws Exception {
        List<double[]> returnsPerAsset = new ArrayList<>();
        int minLen = Integer.MAX_VALUE;

        for (String a : assets) {
            String stooqTicker = STOOQ.get(a);
            if (stooqTicker == null) throw new IllegalArgumentException("Unknown asset: " + a);

            String url = "https://stooq.com/q/d/l/?s=" + stooqTicker + "&i=d";
            List<Double> closes = new ArrayList<>();

            try (InputStreamReader reader = new InputStreamReader(new URL(url).openStream())) {
                Iterable<CSVRecord> records = CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .parse(reader);
                for (CSVRecord r : records) {
                    closes.add(Double.parseDouble(r.get("Close")));
                }
            }

            int keep = Math.min(closes.size(), yearsBack * 252);
            closes = closes.subList(closes.size() - keep, closes.size());

            double[] rets = new double[closes.size() - 1];
            for (int i = 1; i < closes.size(); i++) {
                rets[i - 1] = closes.get(i) / closes.get(i - 1) - 1.0;
            }

            returnsPerAsset.add(rets);
            minLen = Math.min(minLen, rets.length);
        }

        // Align lengths
        for (int i = 0; i < returnsPerAsset.size(); i++) {
            double[] r = returnsPerAsset.get(i);
            returnsPerAsset.set(i, Arrays.copyOfRange(r, r.length - minLen, r.length));
        }

        int n = assets.size();
        double[] mu = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (double r : returnsPerAsset.get(i)) sum += r;
            mu[i] = sum / minLen;
        }

        double[][] cov = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                cov[i][j] = covariance(returnsPerAsset.get(i), returnsPerAsset.get(j));
            }
        }

        return new ReturnStats(mu, cov, returnsPerAsset);
    }

    private double covariance(double[] x, double[] y) {
        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);
        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += (x[i] - meanX) * (y[i] - meanY);
        }
        return sum / (x.length - 1);
    }
}
