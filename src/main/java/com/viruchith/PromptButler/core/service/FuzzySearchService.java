package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight fuzzy ranking using bounded Levenshtein distance over title and tags.
 */
public final class FuzzySearchService {

    public List<PromptTemplate> rank(String query, List<PromptTemplate> templates) {
        Objects.requireNonNull(templates, "templates");
        if (query == null) {
            query = "";
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            ArrayList<PromptTemplate> copy = new ArrayList<PromptTemplate>(templates);
            Collections.sort(copy, (a, b) -> {
                // Favorites first, then alphabetical
                if (a.isFavorite() != b.isFavorite()) {
                    return a.isFavorite() ? -1 : 1;
                }
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            });
            return copy;
        }
        ArrayList<Scored> scored = new ArrayList<Scored>();
        int cutoff = relevanceCutoff(q.length());
        for (PromptTemplate t : templates) {
            String title = t.getTitle().toLowerCase(Locale.ROOT);
            int tier = matchTier(q, title, t);
            int best = bestDistance(q, title, t.getTags(), cutoff + 1);
            if (tier == 5 && best > cutoff) {
                continue;
            }
            scored.add(new Scored(t, tier, best));
        }
        Collections.sort(scored, (a, b) -> {
            if (a.tier != b.tier) {
                return Integer.compare(a.tier, b.tier);
            }
            // Favorites first at same score
            if (a.score != b.score) {
                return Integer.compare(a.score, b.score);
            }
            if (a.template.isFavorite() != b.template.isFavorite()) {
                return a.template.isFavorite() ? -1 : 1;
            }
            return a.template.getTitle().compareToIgnoreCase(b.template.getTitle());
        });
        ArrayList<PromptTemplate> out = new ArrayList<PromptTemplate>();
        for (Scored s : scored) {
            out.add(s.template);
        }
        return out;
    }

    private static int matchTier(String queryLower, String titleLower, PromptTemplate template) {
        if (titleLower.startsWith(queryLower)) {
            return 0;
        }
        if (titleLower.contains(queryLower)) {
            return 1;
        }
        for (String tag : template.getTags()) {
            String normalized = normalize(tag);
            if (normalized.startsWith(queryLower)) {
                return 2;
            }
        }
        for (String tag : template.getTags()) {
            String normalized = normalize(tag);
            if (normalized.contains(queryLower)) {
                return 3;
            }
        }
        String body = normalize(template.getBody());
        if (body.contains(queryLower)) {
            return 4;
        }
        return 5;
    }

    private static int bestDistance(String queryLower, String titleLower, List<String> tags, int maxDistance) {
        int best = levenshteinBounded(queryLower, titleLower, maxDistance);
        for (String tag : tags) {
            String normalized = normalize(tag);
            int d = levenshteinBounded(queryLower, normalized, maxDistance);
            if (d < best) {
                best = d;
            }
            if (best == 0) {
                return 0;
            }
        }
        return best;
    }

    private static int relevanceCutoff(int queryLength) {
        int baseline = queryLength <= 4 ? 2 : (queryLength <= 8 ? 3 : 4);
        return Math.max(2, baseline);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    /**
     * Bounded Levenshtein distance (two-row DP). Returns large value if lengths diverge too far.
     */
    int levenshtein(String a, String b) {
        return levenshteinBounded(a, b, Integer.MAX_VALUE / 2);
    }

    private static int levenshteinBounded(String a, String b, int maxDistance) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("null argument");
        }
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > maxDistance) {
            return maxDistance + 1;
        }
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            int rowBest = curr[0];
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                int del = prev[j] + 1;
                int ins = curr[j - 1] + 1;
                int sub = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(del, ins), sub);
                if (curr[j] < rowBest) {
                    rowBest = curr[j];
                }
            }
            if (rowBest > maxDistance) {
                return maxDistance + 1;
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }

    private static final class Scored {
        private final PromptTemplate template;
        private final int tier;
        private final int score;

        private Scored(PromptTemplate template, int tier, int score) {
            this.template = template;
            this.tier = tier;
            this.score = score;
        }
    }
}
