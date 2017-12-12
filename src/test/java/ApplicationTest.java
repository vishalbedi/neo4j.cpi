import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class ApplicationTest {
    Application application = new Application();
    @Test
    public void start() throws Exception {
        application.start(8);
    }

}