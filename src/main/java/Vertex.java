import java.util.ArrayList;
import java.util.List;

public class Vertex {
    private int id;
    private String label;
    private String name;
    private List<Vertex> connectedTo;

    Vertex(int id, String label, String name){
        this.id = id;
        this.label = label;
        this.name = name;
        connectedTo = new ArrayList<>();
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
        this.connectedTo = vertex.getConnections();
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



    public int getDegree(){
        return connectedTo.size();
    }

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
}
