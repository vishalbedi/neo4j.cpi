import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationPropertiesTest {
    String validFileName = "test.config.properties";
    ApplicationProperties applicationProperties = new ApplicationProperties(validFileName);
    @Test
    public void neo4JDbPath() throws Exception {
        assertEquals("/home/vishal/dev/proteins/databases/graph.db",
                applicationProperties.Neo4JDbPath());
    }

    @Test
    public void proteinsFolderPath() throws Exception {
        assertEquals("/home/vishal/dev/courses/CSCI729/HW4/Proteins/target",
                applicationProperties.ProteinsTargetPath());
    }

    @Test
    public void proteinsQueryPath() throws Exception {
        assertEquals("/home/vishal/dev/courses/CSCI729/HW4/Proteins/query",
                applicationProperties.ProteinsQueryPath());
    }

    @Test
    public void proteinsGroundTruthPath() throws Exception {
        assertEquals("/home/vishal/dev/courses/CSCI729/HW4/Proteins/ground_truth",
                applicationProperties.ProteinsGroundTruthPath());
    }

}