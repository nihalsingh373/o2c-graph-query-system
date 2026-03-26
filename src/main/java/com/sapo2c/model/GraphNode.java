package com.sapo2c.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "graph_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "label", length = 200)
    private String label;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
