package com.example.studyvault.service;

import java.util.Arrays;

/** Deterministic keyless fallback for local development and offline use. */
public class LocalSummaryProvider implements AiSummaryProvider {
  @Override
  public String summarize(String noteContent) {
    String plain =
        noteContent == null
            ? ""
            : noteContent
                .replaceAll("!\\[[^]]*\\]\\([^)]*\\)", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[#*_`~>]", "")
                .replaceAll("\\s+", " ")
                .trim();
    if (plain.isBlank()) return "No content to summarize.";
    String[] sentences = plain.split("(?<=[.!?])\\s+");
    String summary = Arrays.stream(sentences).limit(3).reduce((a, b) -> a + " " + b).orElse(plain);
    if (summary.length() > 500) summary = summary.substring(0, 497).trim() + "...";
    return summary;
  }
}
