package org.opentripplanner.standalone.datastore.gcs;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class BlobPathTest {

    @Test
    public void parse() throws URISyntaxException {
        assertEquals("gs://ole/brum", BlobPath.parse(new URI("gs://ole/brum")).toString());
    }
}