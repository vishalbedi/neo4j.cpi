import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Application {

    ApplicationProperties applicationProperties = null;
    FileHelper fileHelper = null;
    GraphDatabaseService graphDb;

    public Application(){
        applicationProperties = new ApplicationProperties(Constants.APPLICATION_PROPERTIES_FILE);
        fileHelper = new FileHelper();
        graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(applicationProperties.Neo4JDbPath()))
                .setConfig(GraphDatabaseSettings.pagecache_memory, "512M" )
                .setConfig(GraphDatabaseSettings.string_block_size, "60" )
                .setConfig(GraphDatabaseSettings.array_block_size, "300" )
                .newGraphDatabase();

    }
    public void start(int numberOfNodes){

        List<String> targetFileNames = fileHelper.getAllFileNames(applicationProperties.ProteinsTargetPath())
                .map(file -> file.getName().split(Pattern.quote("."))[0]).collect(Collectors.toList());
        List<File> queryFiles = fileHelper.getAllFileNames(applicationProperties.ProteinsQueryPath(),
                numberOfNodes).collect(Collectors.toList());
        List<File> groundTruthFiles = fileHelper.getAllFileNames(applicationProperties.ProteinsGroundTruthPath(),
                numberOfNodes).collect(Collectors.toList());

        groundTruthFiles
                .forEach(gt_file->targetFileNames
                        .forEach(targetFile-> queryFiles
                                .forEach(queryFile -> checkCpiAgainstGroundTruth(queryFile, gt_file, targetFile))));
        graphDb.shutdown();
    }

    private void checkCpiAgainstGroundTruth(File queryFile, File groundTruthFile, String targetFileName){
        System.out.println("=======================================================================================");
        System.out.println("Query File: " + queryFile.getName());
        System.out.println("Target File: " + targetFileName);
        System.out.println("CPI computation Started ");
        try ( Transaction tx = graphDb.beginTx() ){
            Map<Integer, Set<Integer>> cpiMap = this.getCpiMap(queryFile,targetFileName);
            Map<Integer, Set<Integer>> groundTruthMap = fileHelper.readGroundTruth(groundTruthFile,
                    targetFileName,queryFile.getName());
            List<Boolean> check = new ArrayList<>();
            for (int key : cpiMap.keySet()) {
                check.add(cpiMap.get(key).containsAll(groundTruthMap.get(key)));
            }
            System.out.println("ALL MATCH : " + !check.contains(false));

            tx.success();
            tx.close();
        }

    }

    private Map<Integer, Set<Integer>> getCpiMap(File queryFile, String targetFileName){
        QueryGraph queryGraph = new QueryGraph(applicationProperties, queryFile, fileHelper, targetFileName);
        CPI cpi = new CPI(queryGraph, graphDb);
        cpi.computeCPI();
        Map<Integer, Set<Integer>> cpiMap = cpi.getCPIMap();
        cpi.deleteCPINodes();
        return cpiMap;
    }
}
