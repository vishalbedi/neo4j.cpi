import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.util.stream.Collectors;

public class CPITest {

    String validFileName = "config.properties";
    ApplicationProperties applicationProperties = new ApplicationProperties(validFileName);
    FileHelper fileHelper = new FileHelper(applicationProperties);
    File queryFile = fileHelper.getAllFileNames(applicationProperties.ProteinsQueryPath(),8).collect(Collectors.toList()).get(3);
    String targetFilename = "backbones_1AF7";
    QueryGraph queryGraph = new QueryGraph(applicationProperties, queryFile, fileHelper, targetFilename);

    GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(applicationProperties.Neo4JDbPath())).
    setConfig(GraphDatabaseSettings.pagecache_memory, "512M" ).
    setConfig(GraphDatabaseSettings.string_block_size, "60" ).
    setConfig(GraphDatabaseSettings.array_block_size, "300" ).
    newGraphDatabase();

    CPI cpi = new CPI(applicationProperties, queryGraph, db);
    @Test
    public void rootSelection() {
        System.out.println("query file = "+queryFile.getName());
        try ( Transaction tx = db.beginTx() ){
            cpi.rootSelection();
        }
    }
}