package com.alekhya.ecommerce.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiClient.class);

    private final RestClient restClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiAiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
    }

    @Override
    public String chat(String system, String user) {
        String key = properties.apiKey();
        if (key == null || key.isBlank()) {
            log.warn("GEMINI_API_KEY is not configured; the bot will use fallback messages.");
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }
        key = key.trim();
        String body = buildRequestBody(system, user);
        try {
            String response = postForText(key, body);
            return extractText(response);
        } catch (RestClientResponseException exception) {
            log.error("Gemini returned {} {}: {}", exception.getStatusCode(), exception.getStatusText(),
                    exception.getResponseBodyAsString());
            throw exception;
        } catch (Exception exception) {
            log.error("Gemini call failed: {}", exception.getMessage(), exception);
            throw exception;
        }
    }

    private String postForText(String key, String body) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", key)
                        .build(properties.model()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    private String buildRequestBody(String system, String user) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode systemParts = root.putObject("systemInstruction").putArray("parts");
        systemParts.addObject().put("text", system);
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", user);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Gemini request body.", exception);
        }
    }

    String extractText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidate = root.path("candidates").path(0);
            // Gemini may split one answer across several parts; skip "thought" parts from thinking models.
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false)) {
                    builder.append(part.path("text").asText(""));
                }
            }
            String text = builder.toString();
            String finishReason = candidate.path("finishReason").asText("");
            if (!finishReason.isEmpty() && !"STOP".equals(finishReason)) {
                log.warn("Gemini finished with reason {}; reply may be incomplete.", finishReason);
            }
            if (text.isBlank()) {
                log.warn("Gemini returned an empty text field; raw response: {}", response);
                throw new IllegalStateException("Gemini returned an empty response.");
            }
            return text;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse Gemini response.", exception);
        }
    }
}
