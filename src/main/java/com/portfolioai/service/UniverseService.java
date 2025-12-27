package com.portfolioai.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UniverseService {

    private final Map<String, List<String>> sectorMap;
    private final Set<String> sp500Tickers;
    private final Set<String> allowedTickers;

    private final ObjectMapper mapper = new ObjectMapper();

    public UniverseService() {
        // Load everything safely (never throw for blank/missing files)
        this.sectorMap = loadCustomSectorsSafe("universe/custom_sectors.json");
        this.sp500Tickers = loadSp500Safe("universe/sp500.csv");

        // allowed = SP500 ∪ all sector lists
        Set<String> allowed = new HashSet<>();
        allowed.addAll(sp500Tickers);
        for (List<String> lst : sectorMap.values()) allowed.addAll(lst);

        // Normalize to uppercase
        this.allowedTickers = allowed.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Optional logs
        System.out.println("Universe loaded:");
        System.out.println("- sectors: " + sectorMap.keySet());
        System.out.println("- sp500 tickers: " + sp500Tickers.size());
        System.out.println("- allowed tickers: " + allowedTickers.size());
    }

    public Set<String> getAllowedTickers() {
        return allowedTickers;
    }

    public Map<String, List<String>> getSectorMap() {
        return sectorMap;
    }

    public List<String> getTickersForSector(String sector) {
        if (sector == null) return List.of();
        return sectorMap.getOrDefault(sector.toLowerCase(), List.of());
    }

    public boolean isAllowed(String ticker) {
        if (ticker == null) return false;
        return allowedTickers.contains(ticker.trim().toUpperCase());
    }

    // ✅ ADD THIS (tags for recommender)
    public Map<String, String> getTickerTags() {
        Map<String, String> tagByTicker = new HashMap<>();

        // Tag all SP500 as "sp500"
        for (String t : sp500Tickers) {
            if (t != null && !t.isBlank()) {
                tagByTicker.put(t.trim().toUpperCase(), "sp500");
            }
        }

        // Override tags for custom sectors (tech/media/sport/fashion/food)
        for (var entry : sectorMap.entrySet()) {
            String sector = entry.getKey(); // already lowercased
            for (String t : entry.getValue()) {
                if (t != null && !t.isBlank()) {
                    tagByTicker.put(t.trim().toUpperCase(), sector);
                }
            }
        }
        return tagByTicker;
    }

    // ---------- Safe loaders ----------

    private Map<String, List<String>> loadCustomSectorsSafe(String classpath) {
        try {
            ClassPathResource res = new ClassPathResource(classpath);
            if (!res.exists()) {
                System.out.println("WARN: Missing " + classpath + " (using empty sector map)");
                return new HashMap<>();
            }

            try (InputStream in = res.getInputStream()) {
                if (in.available() == 0) {
                    System.out.println("WARN: " + classpath + " is blank (using empty sector map)");
                    return new HashMap<>();
                }

                Map<String, List<String>> raw = mapper.readValue(in, new TypeReference<>() {});
                if (raw == null) return new HashMap<>();

                Map<String, List<String>> out = new HashMap<>();
                for (var entry : raw.entrySet()) {
                    String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
                    List<String> vals = entry.getValue() == null ? List.of() : entry.getValue();

                    List<String> cleaned = vals.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(String::toUpperCase)
                            .distinct()
                            .collect(Collectors.toList());

                    if (!key.isEmpty()) out.put(key, cleaned);
                }
                return out;
            }
        } catch (Exception e) {
            System.out.println("WARN: Failed to load custom sectors (" + classpath + "): " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Set<String> loadSp500Safe(String classpath) {
        try {
            ClassPathResource res = new ClassPathResource(classpath);
            if (!res.exists()) {
                System.out.println("WARN: Missing " + classpath + " (using empty sp500 list)");
                return new HashSet<>();
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)
            )) {
                List<String> lines = br.lines().collect(Collectors.toList());
                if (lines.isEmpty()) {
                    System.out.println("WARN: " + classpath + " is blank (using empty sp500 list)");
                    return new HashSet<>();
                }

                Set<String> out = new HashSet<>();
                for (String line : lines) {
                    if (line == null) continue;
                    String s = line.trim();
                    if (s.isEmpty()) continue;
                    if (s.equalsIgnoreCase("ticker")) continue;

                    String first = s.split(",")[0].trim().toUpperCase();
                    if (!first.isEmpty()) out.add(first);
                }
                return out;
            }
        } catch (Exception e) {
            System.out.println("WARN: Failed to load sp500 csv (" + classpath + "): " + e.getMessage());
            return new HashSet<>();
        }
    }
}
