package io.searchbox.cluster;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dogukan Sonmez
 */
public class NodesInfoTest {

    @Test
    public void getURIWithoutNodeAndInfo() {
        NodesInfo nodesInfo = new NodesInfo.Builder().build();
        assertEquals("/_nodes/_all", nodesInfo.getURI());
    }

    @Test
    public void getURIWithOnlyOneNode() {
        NodesInfo nodesInfo = new NodesInfo.Builder().addNode("twitter").build();
        assertEquals("/_nodes/twitter", nodesInfo.getURI());
    }

    @Test
    public void getURIWithOneNodeAndOneInfo() {
        NodesInfo nodesInfo = new NodesInfo.Builder().addNode("twitter").withOs().build();
        assertEquals("/_nodes/twitter/os", nodesInfo.getURI());
    }

    @Test
    public void getURIWithOnlyOneType() {
        NodesInfo nodesInfo = new NodesInfo.Builder().withOs().build();
        assertEquals("/_nodes/_all/os", nodesInfo.getURI());
    }

    @Test
    public void getURIWithOnlyMultipleNode() {
        NodesInfo nodesInfo = new NodesInfo.Builder().addNode("twitter").addNode("searchbox").build();
        assertEquals("/_nodes/twitter,searchbox", nodesInfo.getURI());
    }

    @Test
    public void getURIWithOnlyMultipleType() {
        NodesInfo nodesInfo = new NodesInfo.Builder().withOs().withProcess().build();
        assertEquals("/_nodes/_all/os,process", nodesInfo.getURI());
    }

    @Test
    public void getURIWithMultipleNodeAndTypes() {
        NodesInfo nodesInfo = new NodesInfo.Builder()
                .addNode("twitter")
                .addNode("jest")
                .withOs()
                .withProcess()
                .withSettings()
                .build();
        assertEquals("/_nodes/twitter,jest/os,process,settings", nodesInfo.getURI());
    }

}
