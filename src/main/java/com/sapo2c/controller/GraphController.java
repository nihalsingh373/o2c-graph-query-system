package com.sapo2c.controller;

import com.sapo2c.dto.GraphDto.*;
import com.sapo2c.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // Get initial graph with sample nodes
    @GetMapping
    public ResponseEntity<GraphResponse> getGraph(
            @RequestParam(defaultValue = "60") int limit) {
        return ResponseEntity.ok(graphService.getInitialGraph(limit));
    }

    // Expand a specific node
    @GetMapping("/expand/{nodeId}")
    public ResponseEntity<ExpandResponse> expandNode(@PathVariable String nodeId) {
        return ResponseEntity.ok(graphService.expandNode(nodeId));
    }

    // Get a single node's details
    @GetMapping("/node/{nodeId}")
    public ResponseEntity<NodeDto> getNode(@PathVariable String nodeId) {
        NodeDto node = graphService.getNode(nodeId);
        if (node == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(node);
    }

    // Get nodes by type
    @GetMapping("/nodes")
    public ResponseEntity<List<NodeDto>> getNodesByType(@RequestParam String type) {
        return ResponseEntity.ok(graphService.getNodesByType(type));
    }

    // Search nodes
    @GetMapping("/search")
    public ResponseEntity<List<NodeDto>> searchNodes(@RequestParam String q) {
        return ResponseEntity.ok(graphService.searchNodes(q));
    }

    // Get subgraph for specific node IDs (for highlighting after LLM response)
    @PostMapping("/subgraph")
    public ResponseEntity<GraphResponse> getSubgraph(@RequestBody List<String> nodeIds) {
        return ResponseEntity.ok(graphService.getSubgraphForNodes(nodeIds));
    }

    // Stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "totalNodes", graphService.getNodeCount(),
            "totalEdges", graphService.getEdgeCount()
        ));
    }
}
