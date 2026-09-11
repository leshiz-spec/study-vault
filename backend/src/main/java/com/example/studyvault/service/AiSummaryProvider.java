package com.example.studyvault.service;

/** Provider boundary for AI-generated note summaries. */
public interface AiSummaryProvider {
    String summarize(String noteContent);
}
