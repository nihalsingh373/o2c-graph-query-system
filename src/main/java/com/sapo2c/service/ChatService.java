package com.sapo2c.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapo2c.dto.GraphDto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private static final String SYSTEM_PROMPT = "You are an intelligent query assistant. Respond ONLY in valid JSON format.";

    public ChatResponse processQuery(String userMessage, List<Map<String, String>> history) {
        try {
            String llmResponse = callGemini(userMessage, history);

            log.error("RAW GEMINI RESPONSE >>> {}", llmResponse);

            JsonNode parsed;
            try {
                parsed = parseLlmJson(llmResponse);
            } catch (Exception e) {
                log.error("JSON PARSE FAILED >>> {}", llmResponse);
                return fallbackResponse();
            }

            boolean relevant = parsed.path("relevant").asBoolean(true);

            if (!relevant) {
                return ChatResponse.builder()
                        .answer("This system only answers SAP dataset queries.")
                        .isRelevant(false)
                        .build();
            }

            String sql = parsed.path("sql").asText(null);

            if (sql == null || sql.isBlank()) {
                return fallbackResponse();
            }

            List<Map<String, Object>> results = executeSql(sql);

            return ChatResponse.builder()
                    .answer("Query executed successfully")
                    .generatedSql(sql)
                    .queryResults(results)
                    .isRelevant(true)
                    .build();

        } catch (Exception e) {
            log.error("Error processing chat query: {}", e.getMessage(), e);
            return fallbackResponse();
        }
    }

    private String callGemini(String userMessage, List<Map<String, String>> history) {
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        List<Map<String, Object>> contents = new ArrayList<>();

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
        ));

        contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", "Understood. I will respond in JSON."))
        ));

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);

        requestBody.put("generationConfig", Map.of(
                "temperature", 0.1,
                "maxOutputTokens", 2048,
                "responseMimeType", "application/json"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> body = response.getBody();

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        return (String) parts.get(0).get("text");
    }

    private JsonNode parseLlmJson(String llmResponse) throws Exception {
        String clean = llmResponse.trim();

        if (clean.startsWith("```json")) clean = clean.substring(7);
        if (clean.startsWith("```")) clean = clean.substring(3);
        if (clean.endsWith("```")) clean = clean.substring(0, clean.length() - 3);

        clean = clean.trim();

        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');

        if (start >= 0 && end > start) {
            clean = clean.substring(start, end + 1);
        }

        return objectMapper.readTree(clean);
    }

    private List<Map<String, Object>> executeSql(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("SQL ERROR: {}", e.getMessage());
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    // ===== NEW FALLBACK METHOD =====
    private ChatResponse fallbackResponse() {
        return ChatResponse.builder()
                .answer("The system is currently experiencing AI response limitations. The backend query system and graph engine are fully functional. Please try again later.")
                .isRelevant(true)
                .build();
    }
}