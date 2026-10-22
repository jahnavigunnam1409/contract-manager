package com.example.contracts.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Basic keyword question answering: splits the contract into sentences, weights each
 * question keyword by rarity (IDF) and returns the best matching sentences.
 */
@Service
public class QaService {

    public record Match(String text, double score) {}
    public record Answer(String answer, List<Match> matches) {}

    private static final Set<String> STOP = Set.of(
        "the","and","for","are","but","not","you","all","any","can","had","her","was","one","our","out",
        "has","him","his","how","its","who","did","get","may","she","too","use","what","when","where",
        "which","while","with","this","that","these","those","there","their","them","then","than","from",
        "have","will","would","could","should","about","into","does","done","been","being","were","shall",
        "tell","give","show","mention","say","says","contract","document","agreement");

    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");

    public Answer ask(String text, String question) {
        List<String> sentences = splitSentences(text);
        Set<String> qTokens = tokens(question);

        if (sentences.isEmpty()) {
            return new Answer("The contract has no readable text.", List.of());
        }
        if (qTokens.isEmpty()) {
            return new Answer("Please ask a more specific question (include some keywords).", List.of());
        }

        List<Set<String>> sTokens = sentences.stream().map(this::tokens).toList();
        int n = sentences.size();

        Map<String, Double> idf = new HashMap<>();
        for (String q : qTokens) {
            long df = sTokens.stream().filter(s -> s.contains(q)).count();
            idf.put(q, Math.log(1.0 + (double) n / (1.0 + df)));
        }

        List<Match> ranked = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double score = 0;
            for (String q : qTokens) {
                if (sTokens.get(i).contains(q)) score += idf.get(q);
            }
            if (score > 0) ranked.add(new Match(sentences.get(i), Math.round(score * 100.0) / 100.0));
        }
        ranked.sort(Comparator.comparingDouble(Match::score).reversed());

        if (ranked.isEmpty()) {
            return new Answer("I couldn't find anything in this contract related to your question.", List.of());
        }
        List<Match> top = ranked.stream().limit(3).collect(Collectors.toList());
        return new Answer(top.get(0).text(), top);
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) return List.of();
        String t = text.replaceAll("[ \\t]+", " ")
                       .replaceAll("(?<!\\n)\\n(?!\\n)", " ");       // single newline -> space
        String[] parts = t.split("(?<=[.!?;:])\\s+|\\n{2,}");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.length() >= 15) out.add(s);
        }
        return out;
    }

    private Set<String> tokens(String s) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = WORD.matcher(s.toLowerCase());
        while (m.find()) {
            String w = m.group();
            if (w.length() < 3 || STOP.contains(w)) continue;
            out.add(stem(w));
        }
        return out;
    }

    /** Very light stemming so "payments" matches "payment", "terminated" matches "terminate". */
    private String stem(String w) {
        if (w.length() > 5 && w.endsWith("ing")) return w.substring(0, w.length() - 3);
        if (w.length() > 4 && w.endsWith("ed")) return w.substring(0, w.length() - 2);
        if (w.length() > 4 && w.endsWith("es")) return w.substring(0, w.length() - 2);
        if (w.length() > 3 && w.endsWith("s")) return w.substring(0, w.length() - 1);
        return w;
    }
}
