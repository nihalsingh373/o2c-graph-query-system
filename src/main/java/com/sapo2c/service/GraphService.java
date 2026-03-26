package com.sapo2c.service;

import com.sapo2c.dto.GraphDto.*;
import com.sapo2c.model.GraphEdge;
import com.sapo2c.model.GraphNode;
import com.sapo2c.repository.GraphEdgeRepository;
import com.sapo2c.repository.GraphNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final GraphNodeRepository nodeRepository;
    private final GraphEdgeRepository edgeRepository;

    // Return a sample of nodes + their edges for initial load
    public GraphResponse getInitialGraph(int limit) {
        // Get a representative sample across node types
        List<String> types = List.of("salesOrder", "customer", "delivery", "billing", "payment", "product");
        List<GraphNode> nodes = new ArrayList<>();

        for (String type : types) {
            List<GraphNode> typeNodes = nodeRepository.findByType(type);
            int take = type.equals("product") ? Math.min(10, typeNodes.size())
                     : Math.min(limit / types.size(), typeNodes.size());
            nodes.addAll(typeNodes.subList(0, take));
        }

        List<String> nodeIds = nodes.stream().map(GraphNode::getId).collect(Collectors.toList());
        List<GraphEdge> edges = edgeRepository.findEdgesForNodes(nodeIds);

        // Only include edges where BOTH nodes are in our set
        Set<String> nodeIdSet = new HashSet<>(nodeIds);
        edges = edges.stream()
            .filter(e -> nodeIdSet.contains(e.getSourceId()) && nodeIdSet.contains(e.getTargetId()))
            .collect(Collectors.toList());

        return GraphResponse.builder()
            .nodes(toNodeDtos(nodes))
            .edges(toEdgeDtos(edges))
            .totalNodes((int) nodeRepository.count())
            .totalEdges((int) edgeRepository.count())
            .build();
    }

    // Expand a specific node — return its neighbors and connecting edges
    public ExpandResponse expandNode(String nodeId) {
        List<GraphEdge> connectedEdges = edgeRepository.findBySourceIdOrTargetId(nodeId, nodeId);

        Set<String> neighborIds = new HashSet<>();
        for (GraphEdge edge : connectedEdges) {
            neighborIds.add(edge.getSourceId());
            neighborIds.add(edge.getTargetId());
        }

        List<GraphNode> neighbors = nodeRepository.findAllById(neighborIds);

        return ExpandResponse.builder()
            .nodes(toNodeDtos(neighbors))
            .edges(toEdgeDtos(connectedEdges))
            .build();
    }

    // Get a specific node's full details
    public NodeDto getNode(String nodeId) {
        return nodeRepository.findById(nodeId)
            .map(this::toNodeDto)
            .orElse(null);
    }

    // Get nodes by type
    public List<NodeDto> getNodesByType(String type) {
        return toNodeDtos(nodeRepository.findByType(type));
    }

    // Search nodes by text
    public List<NodeDto> searchNodes(String query) {
        return toNodeDtos(nodeRepository.searchNodes(query));
    }

    // Highlight specific nodes by ID (used after LLM response)
    public GraphResponse getSubgraphForNodes(List<String> nodeIds) {
        List<GraphNode> nodes = nodeRepository.findAllById(nodeIds);
        List<GraphEdge> edges = edgeRepository.findEdgesForNodes(nodeIds);

        Set<String> nodeIdSet = new HashSet<>(nodeIds);
        edges = edges.stream()
            .filter(e -> nodeIdSet.contains(e.getSourceId()) && nodeIdSet.contains(e.getTargetId()))
            .collect(Collectors.toList());

        return GraphResponse.builder()
            .nodes(toNodeDtos(nodes))
            .edges(toEdgeDtos(edges))
            .totalNodes(nodes.size())
            .totalEdges(edges.size())
            .build();
    }

    public long getNodeCount() { return nodeRepository.count(); }
    public long getEdgeCount() { return edgeRepository.count(); }

    // ========== MAPPERS ==========

    private NodeDto toNodeDto(GraphNode n) {
        return NodeDto.builder()
            .id(n.getId())
            .type(n.getType())
            .label(n.getLabel())
            .metadata(n.getMetadata())
            .build();
    }

    private List<NodeDto> toNodeDtos(List<GraphNode> nodes) {
        return nodes.stream().map(this::toNodeDto).collect(Collectors.toList());
    }

    private List<EdgeDto> toEdgeDtos(List<GraphEdge> edges) {
        return edges.stream().map(e -> EdgeDto.builder()
            .id(e.getId())
            .source(e.getSourceId())
            .target(e.getTargetId())
            .relationship(e.getRelationship())
            .build()).collect(Collectors.toList());
    }
}
