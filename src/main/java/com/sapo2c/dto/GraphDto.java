package com.sapo2c.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class GraphDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDto {
        private String id;
        private String type;
        private String label;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDto {
        private Long id;
        private String source;
        private String target;
        private String relationship;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphResponse {
        private List<NodeDto> nodes;
        private List<EdgeDto> edges;
        private int totalNodes;
        private int totalEdges;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpandResponse {
        private List<NodeDto> nodes;
        private List<EdgeDto> edges;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String message;
        private List<Map<String, String>> history;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private String answer;
        private String generatedSql;
        private List<Map<String, Object>> queryResults;
        private List<String> referencedNodeIds;
        private boolean isRelevant;
    }
}
