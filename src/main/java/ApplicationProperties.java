import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class ApplicationProperties {
    private InputStream input;
    private Properties prop = new Properties();

    private final String NEO4JDEFAULTPATH = "/var/lib/neo4j/data";
    private final String DEFAULTPROTEINSFOLDERPATH = "/home/vishal/dev/courses/CSCI729/HW4/Proteins";
    private final String DEFAULTQUERYPATH ="/home/vishal/dev/courses/CSCI729/HW4/Proteins";
    private final String DEFAULTGROUNDTRUTHPATH = "/home/vishal/dev/courses/CSCI729/HW4/Proteins";


    ApplicationProperties(String fileName) {
        try {
            input = getClass().getClassLoader().getResourceAsStream(fileName);
            if (input != null)
                prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    String Neo4JDbPath(){
        return prop.getProperty("neo4jdbpath",NEO4JDEFAULTPATH);
    }

    String ProteinsTargetPath(){
        return prop.getProperty("proteinsfolderpath",DEFAULTPROTEINSFOLDERPATH);
    }

    String ProteinsQueryPath(){
        return prop.getProperty("proteinsquerypath",DEFAULTQUERYPATH);
    }
    String ProteinsGroundTruthPath(){
        return prop.getProperty("proteingroundtruthpath",DEFAULTGROUNDTRUTHPATH);
    }
}
