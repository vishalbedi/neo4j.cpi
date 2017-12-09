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
        root.addCandidateNodes(rootCandidates);
        Map<Integer, List<Vertex>> levelTree = queryGraph.getLevelTree(root);
        addCountAttribute();
        for(int level = 2; level < levelTree.size()+1; level++){
            for (Vertex level_u : levelTree.get(level)){
                int COUNT = 0;
                for(Vertex neighbor_u : level_u.getConnections()){
                    if(!neighbor_u.getVisited() && levelTree.get(level).contains(neighbor_u)){
                        level_u.addUN(neighbor_u);
                    }
                    else if(neighbor_u.getVisited()){
                        List<Node> candidates_v_of_neighbor_u = candidateComputation(neighbor_u);
                        for (Node v_dash : candidates_v_of_neighbor_u){
                            List<Node> qualifyingNodes = getQualifyingNodes(v_dash, level_u);
                            for (Node v: qualifyingNodes) {
                                int v_count = (int)v.getProperty("cnt");
                                if(v_count == COUNT){
                                    v.setProperty("cnt", v_count+1);
                                }
                            }
                        }
                        COUNT++;
                    }
                }
                for (Node node:
                     getNodesWithCount(COUNT)) {
                    if(candVerify(node,level_u)){
                        level_u.addCandidateNode(node);
                    }
                }
                level_u.setVisited(true);
                addCountAttribute();//reset count to zero
            }
            List<Vertex> levelVertices = levelTree.get(level);
            for (int i = levelVertices.size(); i >=0 ; i-- ){
                Vertex u = levelVertices.get(i);
                int COUNT = 0;
                for(Vertex u_dash : u.getUN()){
                    for (Node v_dash: u_dash.getCandidateNodes()) {
                        List<Node> qualifyingNodes = getQualifyingNodes(v_dash,u);
                        for (Node v :
                                qualifyingNodes) {
                            int v_count = (int) v.getProperty("cnt");
                            if (v_count == COUNT) {
                                v.setProperty("cnt", v_count + 1);
                            }
                        }
                    }
                    COUNT++;
                }
                for (Node v :
                        u.getCandidateNodes()) {
                    int v_count = (int) v.getProperty("cnt");
                    if (v_count != COUNT)
                        u.removeCandidateNode(v);
                }
            }
            addCountAttribute();//reset count to zero
        }
    }

    private List<Node> getNodesWithCount(int count){
        List<Node> candidates = new ArrayList<>();
        for (Node n:
                db.getAllNodes()) {
            if((int)n.getProperty("cnt") == count)
                candidates.add(n);
        }
        return candidates;
    }
    private void addCountAttribute(){
        try ( Transaction tx = db.beginTx() ){
           for(Node n: db.getAllNodes()){
               n.setProperty("cnt",0);
           }
           tx.success();
        }
    }

    private List<Node> getQualifyingNodes(Node v_dash, Vertex level_u){
        List<Node> qualifyingNodes = new ArrayList<>();
        for(Relationship r : v_dash.getRelationships(Direction.INCOMING)){
            Node neighbor = r.getOtherNode(v_dash);
            if (neighbor.getLabels().iterator().next().equals(level_u.getLabel()) && (neighbor.getDegree(Direction.INCOMING) >= level_u.getDegree())){
               qualifyingNodes.add(neighbor);
            }
        }
        return qualifyingNodes;
    }
}
