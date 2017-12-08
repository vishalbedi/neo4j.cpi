import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class QueryGraph {
    private ApplicationProperties prop;
    private File QueryFile;
    private FileHelper fileHelper;
    private String targetFileName;
    Map<Integer, Vertex> SearchQueryVertices = new HashMap<>();
    private List<Vertex> core;
    private List<Vertex> forest;
    private List<Vertex> leaf;

    public QueryGraph(ApplicationProperties prop, File queryFile, FileHelper fileHelper, String targetFileName) {
        this.QueryFile = queryFile;
        this.fileHelper = fileHelper;
        this.prop = prop;
        this.targetFileName = targetFileName;
        this.create();
    }

    private void generateQueryGraph(File file, String target) {
        List<String[]> proteinQueryFile = fileHelper.readFile(file, " ");
        if (file.isFile() && proteinQueryFile != null) {
            readQueryGraph(target, proteinQueryFile);
        }
    }

    private void computeDecomposotions(){
        List<Vertex> queryVertices = deepCopy(SearchQueryVertices.values().stream().collect(Collectors.toList()));
        core = computeCore(queryVertices);

        queryVertices = deepCopy(SearchQueryVertices.values().stream().collect(Collectors.toList()));
        leaf = computeLeaf(queryVertices);

        queryVertices = deepCopy(SearchQueryVertices.values().stream().collect(Collectors.toList()));
        forest = computeForest(queryVertices);
    }

    private void create(){
        generateQueryGraph(this.QueryFile,this.targetFileName);
        computeDecomposotions();
    }

    public List<Vertex> getCore(){
       return core;
    }

    public List<Vertex> getForest(){
        return forest;
    }

    public List<Vertex> getLeaf (){
        return leaf;
    }

    public List<Vertex> getQueryGraph(){
        return null;
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
            for (Vertex v :
                    vertices) {
                v.removeNeighbor(pruneVertices);
            }
            vertices.sort(Comparator.comparingInt(Vertex::getDegree));
            int degree = vertices.get(0).getDegree();
            if(degree > 1){
                return vertices;
            }else {
               return computeCore(vertices);
            }
        }
    }


    private List<Vertex> deepCopy(List<Vertex> vertices){
        Map<Integer, Vertex> copy = new HashMap<>();
        vertices.forEach(v-> copy.put(v.getId(),new Vertex(v)));
        for (Vertex v :
                vertices) {
            Vertex copyV = copy.get(v.getId());
            List<Integer> neghborIds = v.getConnections().stream().map(vertex -> vertex.getId()).collect(Collectors.toList());
            for (int id :
                 neghborIds) {
                copyV.addNeighbor(copy.get(id));
            }
        }
        return copy.values().stream().collect(Collectors.toList());
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

    private void addEdge(int id1, int id2){
        //As per our input vertices are created before edges are crated so we will always have
        //a vertex present
        Vertex v1 = SearchQueryVertices.get(id1);
        Vertex v2 = SearchQueryVertices.get(id2);
        v1.addNeighbor(v2);
    }

    private void addVertex(int id,String label, String name){
        Vertex v = new Vertex(id,label,name);
        SearchQueryVertices.put(id,v);
    }

    private List<Vertex> computeLeaf(List<Vertex> vertices){
        List<Vertex> leaves = new ArrayList<>();
        for(Vertex v : vertices){
            if(v.getDegree() == 1){
                leaves.add(v);
            }
        }
        return leaves;
    }

    private List<Vertex> computeForest(List<Vertex> vertices){
        List<Vertex> forest = new ArrayList<>();
        List<Integer> coreids = core.stream().map(vertex -> vertex.getId()).collect(Collectors.toList());
        List<Integer> leafids = leaf.stream().map(vertex -> vertex.getId()).collect(Collectors.toList());
        for(Vertex v : vertices){
            if(!coreids.contains(v.getId()) && !leafids.contains(v.getId())){
                forest.add(v);
            }
        }
        return forest;
    }

    public Map<Integer, List<Vertex>> getLevelTree(Vertex root){
        Map<Integer, List<Vertex>> levelTree = new HashMap<>();
        int level = 1;
        List<Vertex> levelVertices = new ArrayList<>();
        levelVertices.add(SearchQueryVertices.get(root.getId()));
        levelTree.put(1, levelVertices);
        Set<Vertex> visited = new HashSet<>();
        visited.add(SearchQueryVertices.get(root.getId()));
        while(visited.size() != SearchQueryVertices.size()){
            level+=1;
            List<Vertex> children = new ArrayList<>();
            for (Vertex p : levelTree.get(level-1)){
                for(Vertex n : p.getConnections()){
                    if(!visited.contains(n)){
                        visited.add(n);
                        children.add(n);
                    }
                }
            }
            levelTree.put(level, children);
        }
        return levelTree;
    }
}