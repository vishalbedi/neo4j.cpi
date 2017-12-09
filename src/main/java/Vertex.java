import org.neo4j.cypher.internal.compiler.v2_3.No;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
    private int id;
    private String label;
    private String name;
    private List<Vertex> connectedTo;
    private boolean visited = false;
    private List<Vertex> N = null;
    private List<Vertex> UN = null;
    private List<Node> candidateNodes = null;

    Vertex(int id, String label, String name){
        this.id = id;
        this.label = label;
        this.name = name;
        connectedTo = new ArrayList<>();
        N = new ArrayList<>();
        UN = new ArrayList<>();
        candidateNodes = new ArrayList<>();
    }

    Vertex(int id, String label){
        this.id = id;
        this.label = label;
        connectedTo = new ArrayList<>();
    }

    Vertex(Vertex vertex){
        this.id = vertex.id;
        this.label = vertex.getLabel();
        this.name = vertex.getName();
        connectedTo = new ArrayList<>();
    }

    public List<Node> getCandidateNodes() {
        return candidateNodes;
    }

    public void addCandidateNode(Node n){
        candidateNodes.add(n);
    }

    public void addCandidateNodes(List<Node> nodes){
        candidateNodes.addAll(nodes);
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<Vertex> getConnections() {
        return connectedTo;
    }

    public boolean getVisited(){ return visited; }

    public void addUN(Vertex v){
        UN.add(v);
    }

    public List<Vertex> getUN() {
        return UN;
    }

    public void removeCandidateNode(Node n){
        candidateNodes.remove(n);
    }

    public int getDegree(){
        return connectedTo.size();
    }

    public void setVisited(boolean value){ visited = true; }

    public void addNeighbor(Vertex vertex){
        if(!connectedTo.contains(vertex))
            connectedTo.add(vertex);
    }

    public void removeNeighbor(Vertex vertex){
        if(connectedTo.contains(vertex)){
            connectedTo.remove(vertex);
        }
    }

    public void removeNeighbor(List<Vertex> vertices){
        for (Vertex v :
                vertices) {
            removeNeighbor(v);
        }
    }

    public int maxNeighborDegree(){
        int max = Integer.MIN_VALUE;
        for(Vertex n : connectedTo){
            if(n.getDegree() > max){
                max = n.getDegree();
            }
        }
        return max;
    }
}
