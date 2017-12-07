import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CPI {

    private final ApplicationProperties applicationProperties;
    private QueryGraph queryGraph;

    public CPI(ApplicationProperties applicationProperties, QueryGraph queryGraph){
        this.applicationProperties = applicationProperties;
        this.queryGraph = queryGraph;
    }

    public boolean candVerify(Node v, Vertex u){
        if (maxNeighborNode(v) < u.maxNeighborDegree() )
            return false;

        List<Node> neighbors = new ArrayList<>();
        for(Relationship r : v.getRelationships(Direction.INCOMING)){
            Node neighbor = r.getOtherNode(v);
            neighbors.add(neighbor);
        }
        for (Node n : neighbors){
            List<String> u_neighbors = u.getConnections().stream().map(vertex -> vertex.getLabel()).collect(Collectors.toList());
            if ( u_neighbors.contains(n.getLabels().iterator().next()) ){
                if(n.getDegree(Direction.INCOMING) < u.getDegree())
                    return false;
            }
        }
        return true;
    }

    public int maxNeighborNode(Node v){
        int max = Integer.MIN_VALUE;
        for(Relationship r : v.getRelationships(Direction.INCOMING)){
            Node neighbor = r.getOtherNode(v);
            if(neighbor.getDegree(Direction.INCOMING) > max){
                max = neighbor.getDegree(Direction.INCOMING);
            }
        }
        return max;
    }
}
