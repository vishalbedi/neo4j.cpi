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
        List<String> u_neighbors = u.getConnections().stream().map(vertex -> vertex.getLabel())
                .collect(Collectors.toList());
        for (String label :
                u_neighbors) {
            int v_neighbor_with_label_count = 0;
            int u_neighbor_with_label_count = 0;
            for(Relationship r : v.getRelationships(Direction.INCOMING)){
                Node neighbor = r.getOtherNode(v);
                if(neighbor.getLabels().iterator().next().name().equals(label))
                    v_neighbor_with_label_count++;
            }
            for (Vertex nu :
                    u.getConnections()) {
                if(nu.getLabel().equals(label))
                    u_neighbor_with_label_count++;
            }

            if(v_neighbor_with_label_count<u_neighbor_with_label_count)
                return false;
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
        Vertex root= null;
        if(core.size() == 1)
            root = core.get(0);
        else{
            Map<Vertex, List<Node>> candidatesList = new HashMap<>();
            for(Vertex v : core){
                candidatesList.put(v, candidateComputation(v));
            }
            double arg_min = Double.MAX_VALUE;
            for(Vertex v : candidatesList.keySet()){
                if ((candidatesList.get(v).size() / v.getDegree()) < arg_min ){
                    arg_min = (candidatesList.get(v).size() / v.getDegree());
                    root = v;
                }
            }
        }
        // forward the root of complete query graph and not core
        return queryGraph.SearchQueryVertices.get(root.getId());
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
        addCpiRootNodes(rootCandidates,root);
        Map<Integer, List<Vertex>> levelTree = queryGraph.getLevelTree(root);
        addCountAttribute();
        for(int level = 2; level < levelTree.size()+1; level++){
            forwardCandidateGeneration(levelTree, level);
            backwardCandidatePruning(levelTree, level);
            adjacencyListCreation(levelTree, level);
        }
    }

    private void addCpiRootNodes(List<Node> rootCandidates, Vertex u){
        try(Transaction tx = db.beginTx()) {
            for (Node node :
                    rootCandidates) {
                int id = (int) node.getProperty("id");
                Node cpiNode = createCPINode(u.getLabel(), u.getName(), id, 1, u.getId());
                u.addCpiCandidateNode(id, cpiNode);
            }
            tx.success();
            tx.close();
        }
    }

    private void adjacencyListCreation(Map<Integer, List<Vertex>> levelTree, int level) {
        try (Transaction tx = db.beginTx()){
            for (Vertex u :
                    levelTree.get(level)) {
                Vertex Up = u.getParent();
                for(Node vp : Up.getCandidateNodes()){
                    int vpId = (int)vp.getProperty("id");
                    Node cpiVp = Up.getCpiCandidateNode().get(vpId);
                    if(cpiVp == null)
                        continue;
                    List<Node> qualifyingNodes = getQualifyingNodes(vp, u, false);
                    for (Node v :
                            qualifyingNodes) {
                        if(u.getCandidateNodes().contains(v)){
                            int id = (int)v.getProperty("id");
                            Node cpiV = createCPINode(u.getLabel(), u.getName(), id, level, u.getId());
                            cpiVp.createRelationshipTo(cpiV, RelationshipType.withName("connected_to"));
                            cpiV.createRelationshipTo(cpiVp, RelationshipType.withName("connected_to"));
                            u.addCpiCandidateNode(id,cpiV);
                        }
                    }
                }
            }
            tx.success();
            tx.close();
        }
    }


    private Node createCPINode(String dataLabel, String name, int id, int level, int candidateOf){
        Node node = db.createNode(Label.label("CPI"));
        node.setProperty("level", level);
        node.setProperty("dataLabel", dataLabel);
        node.setProperty("name", name);
        node.setProperty("id", id);
        node.setProperty("candidateOf", candidateOf);
        return node;
    }

    private void backwardCandidatePruning(Map<Integer, List<Vertex>> levelTree, int level) {
        List<Vertex> levelVertices = levelTree.get(level);
        for (int i = levelVertices.size()-1; i >=0 ; i-- ){
            Vertex u = levelVertices.get(i);
            int COUNT = 0;
            for(Vertex u_dash : u.getUN()){
                try(Transaction tx = db.beginTx()) {
                    for (Node v_dash: u_dash.getCandidateNodes()) {
                        List<Node> qualifyingNodes = getQualifyingNodes(v_dash,u, true);
                        for (Node v : qualifyingNodes) {
                            int v_count = (int) v.getProperty("cnt");
                            if (v_count == COUNT) {
                                v.setProperty("cnt", v_count + 1);
                            }
                        }
                    }
                    tx.success();
                    tx.close();
                    COUNT++;
                }
            }
            int finalCOUNT = COUNT;
            u.getCandidateNodes().removeIf(node -> (int) node.getProperty("cnt") != finalCOUNT);

        }
        addCountAttribute();//reset count to zero
    }

    private void forwardCandidateGeneration(Map<Integer, List<Vertex>> levelTree, int level) {
        for (Vertex level_u : levelTree.get(level)){
            int COUNT = 0;
            for(Vertex neighbor_u : level_u.getConnections()){
                if(!neighbor_u.getVisited() && levelTree.get(level).contains(neighbor_u)){
                    level_u.addUN(neighbor_u);
                }
                else if(neighbor_u.getVisited()){
                    try ( Transaction tx = db.beginTx() ){
                        List<Node> candidates_v_of_neighbor_u = candidateComputation(neighbor_u);
                        for (Node v_dash : candidates_v_of_neighbor_u){
                            List<Node> qualifyingNodes = getQualifyingNodes(v_dash, level_u, true);
                            for (Node v: qualifyingNodes) {
                                int v_count = (int)v.getProperty("cnt");
                                if(v_count == COUNT){
                                    v.setProperty("cnt", v_count+1);
                                }
                            }
                        }
                        tx.success();
                        tx.close();
                        COUNT++;
                    }
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
    }

    private List<Node> getNodesWithCount(int count){
        List<Node> candidates = new ArrayList<>();
        for(String label : queryGraph.SearchQueryVertices.values().stream().map(vertex -> vertex.getLabel())
                .distinct().collect(Collectors.toList())){
            ResourceIterator<Node> nodes = db.findNodes(Label.label(label));
            while (nodes.hasNext()){
                Node n = nodes.next();
                if((int) n.getProperty("cnt") == count)
                    candidates.add(n);
            }
        }
        return candidates;
    }

    private void addCountAttribute(){
        try ( Transaction tx = db.beginTx() ){
            for(String label : queryGraph.SearchQueryVertices.values().stream().map(vertex -> vertex.getLabel())
                    .distinct().collect(Collectors.toList())){
                ResourceIterator<Node> nodes = db.findNodes(Label.label(label));
                while (nodes.hasNext()){
                    Node n = nodes.next();
                    n.setProperty("cnt",0);
                }
            }
            tx.success();
            tx.close();
        }
    }

    private List<Node> getQualifyingNodes(Node v_dash, Vertex level_u, boolean checkDegree){
        List<Node> qualifyingNodes = new ArrayList<>();
        for(Relationship r : v_dash.getRelationships(Direction.INCOMING)){
            Node neighbor = r.getOtherNode(v_dash);
            if (neighbor.getLabels().iterator().next().name().equals(level_u.getLabel()) &&
                    (neighbor.getDegree(Direction.INCOMING) >= level_u.getDegree() || !checkDegree)){
               qualifyingNodes.add(neighbor);
            }
        }
        return qualifyingNodes;
    }
}
