package io.searchbox.snapshot;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

/**
 * @author happyprg(hongsgo@gmail.com)
 */
public class GetSnapshotTest {

    private String repository = "kangsungjeon";
    private String snapshot = "leeseohoo";
    private String snapshot2 = "kangsungjeon";

    @Test
    public void testSnapshotMultipleNames() {
        GetSnapshot getSnapshotRepository = new GetSnapshot.Builder(repository).addSnapshot(Arrays.asList(snapshot, snapshot2)).build();
        assertEquals("/_snapshot/kangsungjeon/leeseohoo,kangsungjeon", getSnapshotRepository.getURI());
    }

    @Test
    public void testSnapshotAll() {
        GetSnapshot getSnapshotRepository = new GetSnapshot.Builder(repository).build();
        assertEquals("/_snapshot/kangsungjeon/_all", getSnapshotRepository.getURI());
    }

}
