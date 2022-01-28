package io.searchbox.snapshot;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

/**
 * @author happyprg(hongsgo@gmail.com)
 */
public class SnapshotStatusTest {

    private String repository = "leeseohoo";
    private String snapshot = "leeseola";
    private String snapshot2 = "kangsungjeon";

    @Test
    public void testSnapshotSingleName() {
        SnapshotStatus snapshotStatus = new SnapshotStatus.Builder(repository).addSnapshot(snapshot).build();
        assertEquals("GET", snapshotStatus.getRestMethodName());
        assertEquals("/_snapshot/leeseohoo/leeseola/_status", snapshotStatus.getURI());
    }

    @Test
    public void testSnapshotMultipleNames() {
        SnapshotStatus snapshotStatus = new SnapshotStatus.Builder(repository).addSnapshot(Arrays.asList(snapshot, snapshot2)).build();
        assertEquals("/_snapshot/leeseohoo/leeseola,kangsungjeon/_status", snapshotStatus.getURI());
    }
}
