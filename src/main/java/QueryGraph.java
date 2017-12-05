import java.io.File;
import java.util.*;

public class QueryGraph {
    private ApplicationProperties prop;
    private File QueryFile;
    private FileHelper fileHelper;
    private String targetFileName;
    Map<Integer, Vertex> SearchQueryVertices = new HashMap<>();
    List<Vertex> core;
    List<Vertex> forest;
    List<Vertex> leaf;

    public QueryGraph(ApplicationProperties prop, File queryFile, FileHelper fileHelper, String targetFileName) {
        this.QueryFile = queryFile;
        this.fileHelper = fileHelper;
        this.prop = prop;
        this.targetFileName = targetFileName;
    }

    private void generateQueryGraph(File file, String target) {
        List<String[]> proteinQueryFile = fileHelper.readFile(file, " ");
        if (file.isFile() && proteinQueryFile != null) {
            readQueryGraph(target, proteinQueryFile);
        }
        List<Vertex> queryVertices = new ArrayList<>();
        SearchQueryVertices.values().forEach(v-> queryVertices.add(new Vertex(v)));
        core = computeCore(queryVertices);
    }

    private List<Vertex> computeCore(List<Vertex> vertices){
        if(vertices.size() == 1 || vertices.size()==2){
            if(vertices.size()==2){
                vertices.remove(0);
            }
            return vertices;
        }else {
            List<Vertex> pruneVertices = new ArrayList<>();
            for (Vertex v: vertices){
                if(v.getDegree() <= 1){
                    pruneVertices.add(v);
                }
            }
            vertices.removeAll(pruneVertices);
//            for (Vertex v :
//                    vertices) {
//                v.removeNeighbor(pruneVertices);
//            }
            vertices.sort(Comparator.comparingInt(Vertex::getDegree));
            int degree = vertices.get(0).getDegree();
            if(degree > 1){
                return vertices;
            }else {
               return computeCore(vertices);
            }
        }
    }


    private void readQueryGraph(String target, List<String[]> proteinQueryFile) {
        int currentLine = 0;
        String[] line = proteinQueryFile.get(currentLine++);
        int numberOfNodes = Integer.parseInt(line[0]);
        while (currentLine - 1 < numberOfNodes) {
            line = proteinQueryFile.get(currentLine++);
            if (line.length < 2) {
                currentLine--;
                continue;
            }
            int id = Integer.parseInt(line[0]);
            String name = line[1];
            addVertex(id,target+":"+name, name);
        }
        while (currentLine < proteinQueryFile.size()){
            line = proteinQueryFile.get(currentLine++);
            if(line.length==2){
                int id1 = Integer.parseInt(line[0]);
                int id2 = Integer.parseInt(line[1]);
                addEdge(id1,id2);
            }
        }
    }

    void addEdge(int id1, int id2){
        //As per our input vertices are created before edges are crated so we will always have
        //a vertex present
        Vertex v1 = SearchQueryVertices.get(id1);
        Vertex v2 = SearchQueryVertices.get(id2);
        v1.addNeighbor(v2);
    }

    void addVertex(int id,String label, String name){
        Vertex v = new Vertex(id,label,name);
        SearchQueryVertices.put(id,v);
    }

}