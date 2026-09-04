package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAiSummaryProviderTest {
    @Test
    void missingKeyUsesLocalFallbackWithoutNetworkCall() {
        var provider = new OpenAiSummaryProvider(new ObjectMapper(), "", "not-a-url", "unused", 1, true);

        String summary = provider.summarize("# Heading\n\nFirst sentence. Second sentence.");

        assertEquals("Heading First sentence. Second sentence.", summary);
    }

    @Test
    void localFallbackLimitsLongDrafts() {
        String content = "word ".repeat(200);
        String summary = new LocalSummaryProvider().summarize(content);

        assertTrue(summary.length() <= 500);
    }
}
