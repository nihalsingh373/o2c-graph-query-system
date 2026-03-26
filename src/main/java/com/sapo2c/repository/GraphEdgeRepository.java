package com.sapo2c.repository;

import com.sapo2c.model.GraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphEdgeRepository extends JpaRepository<GraphEdge, Long> {

    List<GraphEdge> findBySourceId(String sourceId);

    List<GraphEdge> findByTargetId(String targetId);

    List<GraphEdge> findBySourceIdOrTargetId(String sourceId, String targetId);

    @Query("SELECT e FROM GraphEdge e WHERE e.sourceId IN :ids OR e.targetId IN :ids")
    List<GraphEdge> findEdgesForNodes(@Param("ids") List<String> nodeIds);
}
