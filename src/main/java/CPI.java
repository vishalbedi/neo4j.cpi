import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CPI {

    private final ApplicationProperties applicationProperties;
    private QueryGraph queryGraph;
    private GraphDatabaseService db;

    public CPI(ApplicationProperties applicationProperties, QueryGraph queryGraph, GraphDatabaseService db){
        this.applicationProperties = applicationProperties;
        this.queryGraph = queryGraph;
        this.db = db;
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

    public void rootSelection(){
        List<Vertex> core = queryGraph.getCore();
        System.out.println(core.size());
        Vertex root = _rootSelection(core);
        System.out.println("root--> "+root.getLabel());
        computeCPI(root);
    }

    private Vertex _rootSelection(List<Vertex> core){

        if(core.size() == 1)
            return core.get(0);
        else{
            Map<Vertex, List<Node>> candidatesList = new HashMap<>();

            for(Vertex v : core){
                candidatesList.put(v, candidateComputation(v));
            }

            double arg_min = Double.MAX_VALUE;
            Vertex root = null;
            for(Vertex v : candidatesList.keySet()){
                if ((candidatesList.get(v).size() / v.getDegree()) < arg_min ){
                    arg_min = (candidatesList.get(v).size() / v.getDegree());
                    root = v;
                }
            }
            return root;
        }
    }

    private List<Node> candidateComputation(Vertex v){
        List<Node> candidates_v = new ArrayList<>();
        ResourceIterator<Node> iterator = db.findNodes(Label.label(v.getLabel()));
        while(iterator.hasNext()){
            Node n = iterator.next();
            if(candVerify(n, v)){
                candidates_v.add(n);
            }
        }
        return candidates_v;
    }

    private void computeCPI (Vertex root){
        List<Node> rootCandidates = candidateComputation(root);
        root.setVisited(true);
        Map<Integer, List<Vertex>> levelTree = queryGraph.getLevelTree(root);
        for (int i : levelTree.keySet()){
            for( Vertex v : levelTree.get(i)){
                System.out.print(v.getId() +"  ");
            }
            System.out.println();
        }

    }

}
