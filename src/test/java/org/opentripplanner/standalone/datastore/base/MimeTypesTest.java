package org.opentripplanner.standalone.datastore.base;

import org.junit.Test;

import static org.junit.Assert.*;

public class MimeTypesTest {

    @Test
    public void mimeType() {
        assertEquals("application/json", MimeTypes.mimeType("a.json"));
        assertEquals("application/json", MimeTypes.mimeType("a.JsOn"));
        assertEquals("application/zip", MimeTypes.mimeType("a.zip"));
        assertEquals("text/html", MimeTypes.mimeType("a.htm"));
        assertEquals("text/html", MimeTypes.mimeType("a.htML"));
        assertEquals("text/css", MimeTypes.mimeType("a.css"));

        assertNull(MimeTypes.mimeType(".css"));
        assertNull(MimeTypes.mimeType("text"));
    }
}