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

    private static final String SCHEMA_CONTEXT = """
        You are a SQL expert for a SAP Order-to-Cash (O2C) business system. 
        You answer questions ONLY about the following PostgreSQL database schema.
        
        TABLES AND COLUMNS:
        
        sales_order_headers: sales_order, sales_order_type, sales_organization, distribution_channel,
          sold_to_party, creation_date, total_net_amount, transaction_currency,
          overall_delivery_status (A=not started, B=partial, C=complete),
          overall_ord_reltd_billg_status (A=not billed, B=partial, C=fully billed),
          customer_payment_terms, requested_delivery_date, delivery_block_reason, header_billing_block_reason
        
        sales_order_items: sales_order, sales_order_item, material, requested_quantity,
          requested_quantity_unit, net_amount, transaction_currency, material_group,
          production_plant, item_billing_block_reason
        
        sales_order_schedule_lines: sales_order, sales_order_item, schedule_line, confirmed_delivery_date,
          order_quantity_unit, confd_order_qty_by_matl_avail_check
        
        outbound_delivery_headers: delivery_document, actual_goods_movement_date, creation_date,
          delivery_block_reason, overall_goods_movement_status, overall_picking_status, shipping_point
        
        outbound_delivery_items: delivery_document, delivery_document_item, actual_delivery_quantity,
          plant, reference_sd_document (FK to sales_order), reference_sd_document_item, storage_location
        
        billing_document_headers: billing_document, billing_document_type, creation_date,
          billing_document_date, billing_document_is_cancelled, cancelled_billing_document,
          total_net_amount, transaction_currency, company_code, fiscal_year, accounting_document, sold_to_party
        
        billing_document_items: billing_document, billing_document_item, material, billing_quantity,
          net_amount, reference_sd_document (FK to sales_order), reference_sd_document_item
        
        billing_document_cancellations: billing_document, billing_document_type, billing_document_is_cancelled,
          cancelled_billing_document, total_net_amount, company_code, sold_to_party
        
        journal_entry_items_ar: company_code, fiscal_year, accounting_document, accounting_document_item,
          gl_account, reference_document (FK to billing_document), customer,
          amount_in_transaction_currency, transaction_currency, posting_date,
          clearing_date, clearing_accounting_document, financial_account_type
        
        payments_ar: company_code, fiscal_year, accounting_document, accounting_document_item,
          clearing_date, clearing_accounting_document, amount_in_transaction_currency,
          transaction_currency, customer, invoice_reference (FK to billing/journal accounting_document),
          sales_document (FK to sales_order), posting_date
        
        business_partners: business_partner, customer, business_partner_full_name,
          business_partner_name, organization_bp_name1, industry, creation_date
        
        business_partner_addresses: business_partner, address_id, city_name, country,
          postal_code, region, street_name
        
        products: product, product_type, product_group, base_unit, division, gross_weight, net_weight
        
        product_descriptions: product, language, product_description
        
        plants: plant, plant_name, valuation_area, sales_organization
        
        KEY RELATIONSHIPS:
        - sales_order_headers.sold_to_party → business_partners.customer
        - outbound_delivery_items.reference_sd_document → sales_order_headers.sales_order
        - billing_document_items.reference_sd_document → sales_order_headers.sales_order
        - billing_document_headers.accounting_document → journal_entry_items_ar.reference_document
        - journal_entry_items_ar.accounting_document → payments_ar.invoice_reference
        - payments_ar.sales_document → sales_order_headers.sales_order
        
        DELIVERY STATUS CODES: A=Not started, B=Partially delivered, C=Fully delivered
        BILLING STATUS CODES: A=Not billed, B=Partially billed, C=Fully billed
        """;

    private static final String SYSTEM_PROMPT = """
        You are an intelligent query assistant for a SAP Order-to-Cash (O2C) business database.
        
        Your job is to:
        1. Determine if the user's question is related to the O2C business data
        2. If YES: Generate a valid PostgreSQL SQL query, execute it mentally, and explain what it finds
        3. If NO: Politely redirect the user
        
        GUARDRAIL - CRITICAL: You MUST respond with a JSON object in EXACTLY this format:
        {
          "relevant": true or false,
          "explanation": "Why this is or isn't relevant to the dataset",
          "sql": "SELECT ... (only if relevant=true, otherwise null)",
          "answer_template": "The answer to the user's question based on query results (use {RESULTS} as placeholder for actual data)"
        }
        
        If the question is NOT about the O2C business data (e.g., general knowledge, coding, weather, creative writing),
        set relevant=false and sql=null and explain that this system only answers questions about the dataset.
        
        RULES FOR SQL GENERATION:
        - Always LIMIT results to 100 rows maximum
        - Use proper JOINs
        - Handle NULL values with COALESCE where appropriate
        - Use table aliases for readability
        - For "trace" queries, JOIN across the full O2C chain
        - For "broken flow" queries, use LEFT JOINs to find missing links
        - Always return meaningful column aliases
        
        EXAMPLE RELEVANT questions: orders, deliveries, invoices, payments, customers, products, 
        billing documents, journal entries, accounts receivable, sales data, order flows
        
        EXAMPLE IRRELEVANT questions: weather, general coding, history, science, writing essays, jokes
        """ + SCHEMA_CONTEXT;

    public ChatResponse processQuery(String userMessage, List<Map<String, String>> history) {
        try {
            // Step 1: Ask LLM to classify and generate SQL
            String llmResponse = callGemini(userMessage, history);
            log.info("LLM raw response: {}", llmResponse);

            // Step 2: Parse LLM JSON response
            JsonNode parsed = parseLlmJson(llmResponse);
            boolean relevant = parsed.path("relevant").asBoolean(false);

            if (!relevant) {
                return ChatResponse.builder()
                    .answer(parsed.path("explanation").asText(
                        "This system is designed to answer questions related to the SAP Order-to-Cash dataset only. " +
                        "Please ask questions about orders, deliveries, invoices, payments, or customers."))
                    .isRelevant(false)
                    .build();
            }

            String sql = parsed.path("sql").asText(null);
            String answerTemplate = parsed.path("answer_template").asText("Here are the results.");

            if (sql == null || sql.isBlank()) {
                return ChatResponse.builder()
                    .answer("I understood your question but couldn't generate a valid query. Please try rephrasing.")
                    .isRelevant(true)
                    .build();
            }

            // Step 3: Execute SQL safely
            List<Map<String, Object>> results = executeSql(sql);

            // Step 4: Ask LLM to formulate natural language answer from results
            String finalAnswer = generateNaturalAnswer(userMessage, sql, results, answerTemplate);

            // Step 5: Extract referenced node IDs from results for graph highlighting
            List<String> nodeIds = extractNodeIds(results);

            return ChatResponse.builder()
                .answer(finalAnswer)
                .generatedSql(sql)
                .queryResults(results)
                .referencedNodeIds(nodeIds)
                .isRelevant(true)
                .build();

        } catch (Exception e) {
            log.error("Error processing chat query: {}", e.getMessage(), e);
            return ChatResponse.builder()
                .answer("I encountered an error processing your query. Please try again.")
                .isRelevant(true)
                .build();
        }
    }

    private String callGemini(String userMessage, List<Map<String, String>> history) {
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        // Build conversation parts
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add system context as first user message
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "user");
        systemMsg.put("parts", List.of(Map.of("text", SYSTEM_PROMPT)));
        contents.add(systemMsg);

        Map<String, Object> systemResp = new HashMap<>();
        systemResp.put("role", "model");
        systemResp.put("parts", List.of(Map.of("text", "Understood. I will only answer questions about the SAP O2C dataset and always respond in the specified JSON format.")));
        contents.add(systemResp);

        // Add conversation history
        if (history != null) {
            for (Map<String, String> msg : history) {
                Map<String, Object> histMsg = new HashMap<>();
                histMsg.put("role", "user".equals(msg.get("role")) ? "user" : "model");
                histMsg.put("parts", List.of(Map.of("text", msg.get("content"))));
                contents.add(histMsg);
            }
        }

        // Add current message
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("parts", List.of(Map.of("text", userMessage)));
        contents.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        requestBody.put("generationConfig", Map.of(
            "temperature", 0.1,
            "maxOutputTokens", 2048
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> body = response.getBody();

        // Extract text from Gemini response
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }

    private JsonNode parseLlmJson(String llmResponse) throws Exception {
        // Strip markdown code blocks if present
        String clean = llmResponse.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();

        // Find JSON object
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) {
            clean = clean.substring(start, end + 1);
        }

        return objectMapper.readTree(clean);
    }

    private List<Map<String, Object>> executeSql(String sql) {
        try {
            // Safety: only allow SELECT statements
            String normalizedSql = sql.trim().toUpperCase();
            if (!normalizedSql.startsWith("SELECT") && !normalizedSql.startsWith("WITH")) {
                log.warn("Blocked non-SELECT SQL: {}", sql);
                return List.of();
            }

            // Ensure LIMIT exists
            if (!normalizedSql.contains("LIMIT")) {
                sql = sql.trim().replaceAll(";$", "") + " LIMIT 100";
            }

            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("SQL execution error: {}", e.getMessage());
            return List.of(Map.of("error", "Query execution failed: " + e.getMessage()));
        }
    }

    private String generateNaturalAnswer(String question, String sql,
                                          List<Map<String, Object>> results,
                                          String template) {
        if (results.isEmpty()) {
            return "No results found for your query. The dataset may not contain matching records.";
        }

        if (results.size() == 1 && results.get(0).containsKey("error")) {
            return "There was an issue executing the query: " + results.get(0).get("error");
        }

        // For small result sets, ask LLM to summarize
        if (results.size() <= 20) {
            try {
                String resultsJson = objectMapper.writeValueAsString(results);
                String prompt = String.format("""
                    The user asked: "%s"
                    
                    The SQL query was: %s
                    
                    The query returned these results (JSON): %s
                    
                    Please provide a clear, concise natural language answer to the user's question based on these results.
                    Be specific with numbers and names from the data. Keep it under 200 words.
                    Do NOT include SQL in your answer.
                    """, question, sql, resultsJson);

                String summary = callGeminiSimple(prompt);
                return summary;
            } catch (Exception e) {
                log.error("Error generating natural answer: {}", e.getMessage());
            }
        }

        // Fallback: format results directly
        return String.format("Found %d result(s). Here are the details:", results.size());
    }

    private String callGeminiSimple(String prompt) {
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 512)
        );

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

    private List<String> extractNodeIds(List<Map<String, Object>> results) {
        List<String> nodeIds = new ArrayList<>();
        for (Map<String, Object> row : results) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey().toLowerCase();
                Object val = entry.getValue();
                if (val == null) continue;
                String s = String.valueOf(val);
                if (key.contains("sales_order") && !s.isBlank()) nodeIds.add("SO-" + s);
                else if (key.contains("delivery_document") && !s.isBlank()) nodeIds.add("DEL-" + s);
                else if (key.contains("billing_document") && !s.isBlank()) nodeIds.add("BILL-" + s);
                else if (key.contains("accounting_document") && !s.isBlank()) nodeIds.add("JE-" + s);
            }
        }
        return nodeIds.stream().distinct().limit(50).collect(java.util.stream.Collectors.toList());
    }
}
