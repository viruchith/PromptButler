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
        for (PromptTemplate t : templates) {
            int best = levenshtein(q, t.getTitle().toLowerCase(Locale.ROOT));
            for (String tag : t.getTags()) {
                if (tag == null) {
                    continue;
                }
                int d = levenshtein(q, tag.toLowerCase(Locale.ROOT));
                if (d < best) {
                    best = d;
                }
            }
            scored.add(new Scored(t, best));
        }
        Collections.sort(scored, (a, b) -> {
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

    /**
     * Bounded Levenshtein distance (two-row DP). Returns large value if lengths diverge too far.
     */
    int levenshtein(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("null argument");
        }
        int n = a.length();
        int m = b.length();
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
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                int del = prev[j] + 1;
                int ins = curr[j - 1] + 1;
                int sub = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(del, ins), sub);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }

    private static final class Scored {
        private final PromptTemplate template;
        private final int score;

        private Scored(PromptTemplate template, int score) {
            this.template = template;
            this.score = score;
        }
    }
}
