package com.example.studyvault.service;

import com.example.studyvault.exception.AiNotConfiguredException;
import com.example.studyvault.exception.AiProviderException;
import com.example.studyvault.exception.AiRateLimitException;
import com.example.studyvault.exception.AiTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Small OpenAI-compatible adapter. The API key is read only by the backend. */
@Component
public class OpenAiSummaryProvider implements AiSummaryProvider {
  private final ObjectMapper mapper;
  private final HttpClient client;
  private final String apiKey;
  private final String apiUrl;
  private final String model;
  private final Duration timeout;
  private final boolean localFallbackEnabled;
  private final LocalSummaryProvider localFallback = new LocalSummaryProvider();

  public OpenAiSummaryProvider(
      ObjectMapper mapper,
      @Value("${AI_API_KEY:}") String apiKey,
      @Value("${AI_API_URL:https://api.openai.com/v1/chat/completions}") String apiUrl,
      @Value("${AI_MODEL:gpt-4o-mini}") String model,
      @Value("${AI_TIMEOUT_SECONDS:20}") long timeoutSeconds,
      @Value("${AI_LOCAL_FALLBACK:true}") boolean localFallbackEnabled) {
    this.mapper = mapper;
    this.client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)))
            .build();
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.apiUrl = apiUrl;
    this.model = model;
    this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
    this.localFallbackEnabled = localFallbackEnabled;
  }

  @Override
  public String summarize(String noteContent) {
    // Keep summaries useful in local/offline environments. A configured key uses
    // the remote provider; no key uses the deterministic local adapter instead.
    if (apiKey.isBlank()) {
      if (localFallbackEnabled) return localFallback.summarize(noteContent);
      throw new AiNotConfiguredException();
    }
    final String requestJson;
    try {
      requestJson =
          mapper.writeValueAsString(
              Map.of(
                  "model",
                  model,
                  "temperature",
                  0.2,
                  "messages",
                  new Object[] {
                    Map.of(
                        "role",
                        "system",
                        "content",
                        "Summarize the user's note clearly and briefly. Return only the summary."),
                    Map.of("role", "user", "content", noteContent)
                  }));
    } catch (IOException ex) {
      throw new AiProviderException("Unable to prepare the AI request");
    }
    final HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder(URI.create(apiUrl))
              .timeout(timeout)
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestJson))
              .build();
    } catch (IllegalArgumentException ex) {
      throw new AiProviderException("The AI service URL is invalid");
    }
    final HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (java.net.http.HttpTimeoutException ex) {
      throw new AiTimeoutException();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AiTimeoutException();
    } catch (IOException ex) {
      throw new AiProviderException();
    }
    if (response.statusCode() == 408 || response.statusCode() == 504)
      throw new AiTimeoutException();
    if (response.statusCode() == 429) throw new AiRateLimitException();
    if (response.statusCode() == 401 || response.statusCode() == 403)
      throw new AiProviderException("The AI provider rejected the API key");
    if (response.statusCode() == 404)
      throw new AiProviderException("The AI provider endpoint or model was not found");
    if (response.statusCode() == 400)
      throw new AiProviderException("The AI provider rejected the request");
    if (response.statusCode() < 200 || response.statusCode() >= 300)
      throw new AiProviderException();
    try {
      JsonNode content =
          mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
      if (!content.isTextual() || content.asText().isBlank())
        throw new AiProviderException("The AI service returned an empty summary");
      return content.asText().trim();
    } catch (IOException ex) {
      throw new AiProviderException("The AI service returned an invalid response");
    }
  }
}
