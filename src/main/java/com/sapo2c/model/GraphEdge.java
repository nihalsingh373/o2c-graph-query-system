package com.sapo2c.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "graph_edges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false, length = 50)
    private String sourceId;

    @Column(name = "target_id", nullable = false, length = 50)
    private String targetId;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
