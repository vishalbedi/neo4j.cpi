import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationProperties {
    private InputStream input;
    private Properties prop = new Properties();

    private final String NEO4JDEFAULTPATH = "/var/lib/neo4j/data";
    private final String DEFAULTPROTEINSFOLDERPATH = "/home/vishal/dev/courses/CSCI729/HW4/Proteins";
    private final String DEFAULTQUERYPATH ="/home/vishal/dev/courses/CSCI729/HW4/Proteins";
    private final String DEFAULTGROUNDTRUTHPATH = "/home/vishal/dev/courses/CSCI729/HW4/Proteins";

    public ApplicationProperties(String fileName) {
        try {
            input = getClass().getClassLoader().getResourceAsStream(fileName);
            if (input != null)
                prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public String Neo4JDbPath(){
        return prop.getProperty("neo4jdbpath",NEO4JDEFAULTPATH);
    }

    public String ProteinsTargetPath(){
        return prop.getProperty("proteinsfolderpath",DEFAULTPROTEINSFOLDERPATH);
    }

    public String ProteinsQueryPath(){
        return prop.getProperty("proteinsquerypath",DEFAULTQUERYPATH);
    }
    public String ProteinsGroundTruthPath(){
        return prop.getProperty("proteingroundtruthpath",DEFAULTGROUNDTRUTHPATH);
    }
}
