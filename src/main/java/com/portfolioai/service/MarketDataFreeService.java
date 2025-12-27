package com.portfolioai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class MarketDataFreeService {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Cache so you don't refetch on every request
    private final Map<String, List<Double>> closeCache = new ConcurrentHashMap<>();

    /**
     * Loads daily closes for a US stock ticker using Stooq (free).
     * Returns closes in chronological order (oldest -> newest).
     */
    public List<Double> loadDailyClosesUS(String ticker) {
        if (ticker == null) return List.of();
        String t = ticker.trim().toUpperCase();
        if (t.isEmpty()) return List.of();

        // Return cached if available
        List<Double> cached = closeCache.get(t);
        if (cached != null) return cached;

        // Fetch + parse
        List<Double> closes = fetchStooqDailyCloses(t);
        closeCache.put(t, closes);
        return closes;
    }

    /**
     * Convert closes to daily returns: r[t] = close[t]/close[t-1] - 1
     */
    public List<Double> closesToDailyReturns(List<Double> closes) {
        if (closes == null || closes.size() < 2) return List.of();
        List<Double> rets = new ArrayList<>(closes.size() - 1);
        for (int i = 1; i < closes.size(); i++) {
            double prev = closes.get(i - 1);
            double cur = closes.get(i);
            if (prev <= 0 || cur <= 0) continue;
            rets.add((cur / prev) - 1.0);
        }
        return rets;
    }

    /**
     * Simple "health" check: do we have enough data to score?
     */
    public boolean hasEnoughData(List<Double> closes, int minPoints) {
        return closes != null && closes.size() >= minPoints;
    }

    // ---------------- internal helpers ----------------

    private List<Double> fetchStooqDailyCloses(String tickerUpper) {
        try {
            // Stooq expects lowercase + ".us" for US tickers
            String stooqSymbol = tickerUpper.toLowerCase() + ".us";
            String url = "https://stooq.com/q/d/l/?s=" + stooqSymbol + "&i=d";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                System.out.println("WARN: Stooq HTTP " + resp.statusCode() + " for " + tickerUpper);
                return List.of();
            }

            String csv = resp.body();
            if (csv == null || csv.trim().isEmpty()) {
                System.out.println("WARN: Empty CSV from Stooq for " + tickerUpper);
                return List.of();
            }

            // CSV format:
            // Date,Open,High,Low,Close,Volume
            // 2020-01-02,....
            String[] lines = csv.split("\\R");
            if (lines.length <= 1) return List.of();

            List<Double> closes = new ArrayList<>(lines.length - 1);

            // Start at 1 to skip header
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",");
                if (cols.length < 5) continue;

                String closeStr = cols[4].trim();
                if (closeStr.isEmpty() || closeStr.equalsIgnoreCase("null")) continue;

                try {
                    double close = Double.parseDouble(closeStr);
                    if (close > 0) closes.add(close);
                } catch (NumberFormatException ignored) {
                }
            }

            // Stooq returns chronological already (oldest->newest). If not, you can sort by date,
            // but we only stored closes so we assume order is okay.
            if (closes.size() < 50) {
                System.out.println("WARN: Low data count for " + tickerUpper + ": " + closes.size());
            }

            return closes;
        } catch (Exception e) {
            System.out.println("WARN: Failed Stooq fetch for " + tickerUpper + ": " + e.getMessage());
            return List.of();
        }
    }
}
