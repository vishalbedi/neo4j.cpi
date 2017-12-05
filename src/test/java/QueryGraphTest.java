import org.junit.Test;

import java.io.File;
import java.util.stream.Collectors;

public class QueryGraphTest {
    String validFileName = "config.properties";
    ApplicationProperties applicationProperties = new ApplicationProperties(validFileName);
    FileHelper fileHelper = new FileHelper(applicationProperties);
    File queryFile = fileHelper.getAllFileNames(applicationProperties.ProteinsQueryPath(),8).collect(Collectors.toList()).get(0);
    String targetFilename = "backbones_1AF7";
    QueryGraph queryGraph = new QueryGraph(applicationProperties, queryFile, fileHelper, targetFilename);

    @Test
    public void generateQueryGraph() {
        queryGraph.generateQueryGraph(queryFile,targetFilename);
        assert true;
    }
}