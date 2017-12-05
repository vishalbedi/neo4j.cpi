import java.util.ArrayList;
import java.util.Collections;
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
}
