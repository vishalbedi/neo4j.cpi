import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class CPI {

    private QueryGraph queryGraph;
    private GraphDatabaseService db;

    CPI(QueryGraph queryGraph, GraphDatabaseService db){
        this.queryGraph = queryGraph;
        this.db = db;
    }

    private boolean candVerify(Node v, Vertex u){
        if (maxNeighborNode(v) < u.maxNeighborDegree() )
            return false;
        List<String> u_neighbors = u.getConnections().stream().map(Vertex::getLabel)
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

    private int maxNeighborNode(Node v){
        int max = Integer.MIN_VALUE;
        for(Relationship r : v.getRelationships(Direction.INCOMING)){
            Node neighbor = r.getOtherNode(v);
            if(neighbor.getDegree(Direction.INCOMING) > max){
                max = neighbor.getDegree(Direction.INCOMING);
            }
        }
        return max;
    }

    Vertex rootSelection(){
        List<Vertex> core = queryGraph.getCore();
        Vertex root = _rootSelection(core);
        System.out.println("Number of Nodes in Core: " + core.size());
        System.out.println("Query Root Node: " + root.getLabel());
        return root;
    }

    public void computeCPI(){
        Vertex root = rootSelection();
        computeCPI(root);
    }

    private Vertex _rootSelection(List<Vertex> core){
        Vertex root= core.get(0);
        if(core.size() != 1){
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
        Map<Integer, List<Vertex>> levelTree = queryGraph.getLevelTree(root);
        topDownConstruction(root,levelTree);
        bottomUpRefinement(levelTree);
    }


    private void topDownConstruction(Vertex root, Map<Integer, List<Vertex>> levelTree){
        List<Node> rootCandidates = candidateComputation(root);
        root.setVisited(true);
        root.addCandidateNodes(rootCandidates);
        addCpiRootNodes(rootCandidates,root);
        addCountAttribute();
        for(int level = 2; level < levelTree.size()+1; level++){
            forwardCandidateGeneration(levelTree, level);
            backwardCandidatePruning(levelTree, level);
            adjacencyListCreation(levelTree, level);
        }
    }


    public void deleteCPINodes(){
        //Cypher : MATCH (n:CPI) OPTIONAL MATCH (n)-[r]-() DELETE n,r
        try(Transaction tx = db.beginTx()) {
            ResourceIterator<Node> iter = db.findNodes(Label.label(Constants.CPI_LABEL));
            while (iter.hasNext()) {
                Node node = iter.next();
                for (Relationship r : node.getRelationships(Direction.BOTH)) {
                    r.delete();
                }
                node.delete();
            }
            tx.success();
            tx.close();
        }
    }

    private void addCpiRootNodes(List<Node> rootCandidates, Vertex u){
        int rootLevel = 1;
        try(Transaction tx = db.beginTx()) {
            for (Node node :
                    rootCandidates) {
                int id = (int) node.getProperty(Constants.ID_ATTRIBUTE_KEY);
                Node cpiNode = createCPINode(u.getLabel(), u.getName(), id, rootLevel, u.getId());
                u.addCpiCandidateNode(id, cpiNode);
            }
            tx.success();
            tx.close();
        }
    }

    private void adjacencyListCreation(Map<Integer, List<Vertex>> levelTree, int level) {
        try (Transaction tx = db.beginTx()){
            for (Vertex u : levelTree.get(level)) {
                Vertex Up = u.getParent();
                for(Node vp : Up.getCandidateNodes()){
                    int vpId = (int)vp.getProperty(Constants.ID_ATTRIBUTE_KEY);
                    Node cpiVp = Up.getCpiCandidateNode().get(vpId);
                    if(cpiVp == null)
                        continue;
                    List<Node> qualifyingNodes = getQualifyingNodes(vp, u, false);
                    for (Node v : qualifyingNodes) {
                        if(u.getCandidateNodes().contains(v)){
                            int id = (int)v.getProperty(Constants.ID_ATTRIBUTE_KEY);
                            Node cpiV = createCPINode(u.getLabel(), u.getName(), id, level, u.getId());
                            cpiVp.createRelationshipTo(cpiV, RelationshipType.withName(Constants.CONNECTED_TO));
                            cpiV.createRelationshipTo(cpiVp, RelationshipType.withName(Constants.CONNECTED_TO));
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
        Node node = db.createNode(Label.label(Constants.CPI_LABEL));
        node.setProperty(Constants.LEVEL_ATTRIBUTE_KEY, level);
        node.setProperty(Constants.DATA_LABEL_ATTRIBUTE_KEY, dataLabel);
        node.setProperty(Constants.NAME_ATTRIBUTE_KEY, name);
        node.setProperty(Constants.ID_ATTRIBUTE_KEY, id);
        node.setProperty(Constants.CANDIDATE_OF, candidateOf);
        return node;
    }

    private void backwardCandidatePruning(Map<Integer, List<Vertex>> levelTree, int level) {
        List<Vertex> levelVertices = levelTree.get(level);
        for (int i = levelVertices.size()-1; i >=0 ; i-- ){
            Vertex u = levelVertices.get(i);
            int COUNT = 0;
            for(Vertex u_dash : u.getUN()){
                COUNT = candidateNeighborFilter(u, COUNT, u_dash);
            }
            int finalCOUNT = COUNT;
            u.getCandidateNodes().removeIf(node -> (int) node.getProperty(Constants.COUNT_ATTRIBUTE_KEY) != finalCOUNT);

        }
        addCountAttribute();//reset count to zero
    }

    private void forwardCandidateGeneration(Map<Integer, List<Vertex>> levelTree, int level) {
        for (Vertex level_u : levelTree.get(level)){
            int COUNT = 0;
            for(Vertex u_dash : level_u.getConnections()){
                if(!u_dash.getVisited() && levelTree.get(level).contains(u_dash)){
                    level_u.addUN(u_dash);
                }
                else if(u_dash.getVisited()){
                    COUNT = candidateNeighborFilter(level_u, COUNT, u_dash);
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

    private int candidateNeighborFilter(Vertex level_u, int COUNT, Vertex u_dash) {
        try ( Transaction tx = db.beginTx() ){
            for (Node v_dash : u_dash.getCandidateNodes()){
                List<Node> qualifyingNodes = getQualifyingNodes(v_dash, level_u, true);
                for (Node v: qualifyingNodes) {
                    int v_count = (int)v.getProperty(Constants.COUNT_ATTRIBUTE_KEY);
                    if(v_count == COUNT){
                        v.setProperty(Constants.COUNT_ATTRIBUTE_KEY, v_count+1);
                    }
                }
            }
            tx.success();
            tx.close();
            COUNT++;
        }
        return COUNT;
    }

    private List<Node> getNodesWithCount(int count){
        List<Node> candidates = new ArrayList<>();
        for(String label : queryGraph.SearchQueryVertices.values().stream().map(Vertex::getLabel)
                .distinct().collect(Collectors.toList())){
            ResourceIterator<Node> nodes = db.findNodes(Label.label(label));
            while (nodes.hasNext()){
                Node n = nodes.next();
                if((int) n.getProperty(Constants.COUNT_ATTRIBUTE_KEY) == count)
                    candidates.add(n);
            }
        }
        return candidates;
    }

    private void addCountAttribute(){
        try ( Transaction tx = db.beginTx() ){
            for(String label : queryGraph.SearchQueryVertices.values().stream().map(Vertex::getLabel)
                    .distinct().collect(Collectors.toList())){
                ResourceIterator<Node> nodes = db.findNodes(Label.label(label));
                while (nodes.hasNext()){
                    Node n = nodes.next();
                    n.setProperty(Constants.COUNT_ATTRIBUTE_KEY,0);
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


    private void bottomUpRefinement(Map<Integer, List<Vertex>> levelTree){
        for(int level = levelTree.size(); level >0 ; level--){
            if(level == levelTree.size())
                continue;
            for(Vertex u : levelTree.get(level)){
                int COUNT = 0;
                for(Vertex u_dash : levelTree.get(level+1)) {
                    if (u.getConnections().contains(u_dash)) {
                        COUNT = candidateNeighborFilter(u, COUNT, u_dash);
                    }
                }
                List<Node> candidates_of_u = u.getCandidateNodes();

                int finalCount = COUNT;
                candidates_of_u.removeIf(node -> (int) node.getProperty(Constants.COUNT_ATTRIBUTE_KEY) != finalCount);

                addCountAttribute();        // reset cnt to 0
                for(Node v : candidates_of_u){
                    for(Vertex u_dash : levelTree.get(level+1)) {
                        if (u_dash.getParent().getId() == u.getId()) {
                            List<Integer> u_dashCandidateIds = u_dash.getCandidateNodes()
                                    .stream().map(node -> (int)node.getProperty(Constants.ID_ATTRIBUTE_KEY))
                                    .collect(Collectors.toList());
                            try(Transaction tx = db.beginTx()){
                                for (Node v_dash :
                                        getCpiAdjacencyList(v, u, u_dash)) {
                                    int v_dashId = (int) v_dash.getProperty(Constants.ID_ATTRIBUTE_KEY);
                                    if(!u_dashCandidateIds.contains(v_dashId)){
                                        for (Relationship r : v_dash.getRelationships(Direction.BOTH)) {
                                            r.delete();
                                        }
                                        v_dash.delete();
                                    }
                                }
                                tx.success();
                                tx.close();
                            }
                        }
                    }
                }
            }
        }
    }


    public Map<Integer, Set<Integer>> getCPIMap(){
        Map<Integer, Set<Integer>> cpiMap = new HashMap<>();
        for (Vertex u :
                queryGraph.SearchQueryVertices.values()) {
            cpiMap.put(u.getId(),getCPINodeIds(u));

        }
        return cpiMap;
    }

    private Set<Integer> getCPINodeIds(Vertex u){
        Set<Integer> cpiNodeIds = new HashSet<>();
        ResourceIterator<Node> iterator = db.findNodes(Label.label(Constants.CPI_LABEL),
                Constants.CANDIDATE_OF,u.getId());
        while (iterator.hasNext()){
            Node cpiNode = iterator.next();
            int nodeId = (int) cpiNode.getProperty(Constants.ID_ATTRIBUTE_KEY);
                cpiNodeIds.add(nodeId);
        }
        return cpiNodeIds;

    }
    
    private List<Node> getCpiAdjacencyList(Node v, Vertex u, Vertex u_dash){
        List<Node> adjacencyListV = new ArrayList<>();
        int id = (int) v.getProperty(Constants.ID_ATTRIBUTE_KEY);
        ResourceIterator<Node> itrIterator = db.findNodes(Label.label(Constants.CPI_LABEL),
                Constants.ID_ATTRIBUTE_KEY, id);
        Node cpiV = null;
        while (itrIterator.hasNext()){
            Node node = itrIterator.next();
            if((int)node.getProperty(Constants.CANDIDATE_OF) == u.getId()){
                cpiV = node;
                break;
            }
        }
        if(cpiV == null)
            return  adjacencyListV;
        for (Relationship r :
                cpiV.getRelationships(Direction.INCOMING)) {
            Node otherNode = r.getOtherNode(cpiV);
            if(!adjacencyListV.contains(otherNode) && (int)otherNode
                    .getProperty(Constants.CANDIDATE_OF) == u_dash.getId())
                adjacencyListV.add(otherNode);
        }
        return adjacencyListV;
    }
}
