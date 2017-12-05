import org.junit.Test;

import static org.junit.Assert.*;

public class VertexTest {
    Vertex vertex1 = new Vertex(1,"C","C");
    Vertex vertex2 = new Vertex(2,"B","B");
    @Test
    public void getName() throws Exception {
        assertEquals("C",vertex1.getName());
    }

    @Test
    public void getId() throws Exception {
        assertEquals(1,vertex1.getId());
    }

    @Test
    public void getLabel() throws Exception {
        assertEquals("C", vertex1.getLabel());
    }

    @Test
    public void getConnections() throws Exception {
        vertex1.addNeighbor(vertex2);
        assert(vertex1.getConnections().contains(vertex2));
    }

    @Test
    public void getDegree() throws Exception {
        assertEquals(vertex1.getConnections().size(),vertex1.getDegree());
    }

    @Test
    public void addNeighbor() throws Exception {
        vertex2.addNeighbor(vertex1);
        assert(vertex2.getConnections().contains(vertex1));
    }

}