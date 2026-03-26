package com.sapo2c.repository;

import com.sapo2c.model.GraphNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphNodeRepository extends JpaRepository<GraphNode, String> {

    List<GraphNode> findByType(String type);

    @Query("SELECT n FROM GraphNode n WHERE n.type IN :types")
    List<GraphNode> findByTypes(@Param("types") List<String> types);

    @Query(value = "SELECT * FROM graph_nodes WHERE label ILIKE %:query% OR metadata::text ILIKE %:query%", nativeQuery = true)
    List<GraphNode> searchNodes(@Param("query") String query);
}
