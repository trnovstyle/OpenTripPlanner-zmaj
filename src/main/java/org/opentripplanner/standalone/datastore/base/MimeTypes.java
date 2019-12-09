package org.opentripplanner.standalone.datastore.base;

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
    private static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        mimeTypes.put(".css", "text/css");
        mimeTypes.put(".gz", "application/gzip");
        mimeTypes.put(".htm", "text/html");
        mimeTypes.put(".html", "text/html");
        mimeTypes.put(".ico", "image/vnd.microsoft.icon");
        mimeTypes.put(".ics", "text/calendar");
        mimeTypes.put(".jar", "application/java-archive");
        mimeTypes.put(".jpeg", "image/jpeg");
        mimeTypes.put(".jpg", "image/jpeg");
        mimeTypes.put(".js", "text/javascript");
        mimeTypes.put(".json", "application/json");
        mimeTypes.put(".otf", "font/otf");
        mimeTypes.put(".png", "image/png");
        mimeTypes.put(".pdf", "application/pdf");
        mimeTypes.put(".rtf", "application/rtf");
        mimeTypes.put(".svg", "image/svg+xml");
        mimeTypes.put(".tar", "application/x-tar");
        mimeTypes.put(".tif", "image/tiff");
        mimeTypes.put(".tiff", "image/tiff");
        mimeTypes.put(".ttf", "font/ttf");
        mimeTypes.put(".txt", "text/plain");
        mimeTypes.put(".xml", "text/xml");
        mimeTypes.put(".zip", "application/zip");
    }

    public static String mimeType(String filename) {
        int idx = filename.lastIndexOf('.');
        if(idx <= 0) {
            return null;
        }
        String ext = filename.substring(idx).toLowerCase();

        return mimeTypes.get(ext);
    }
}
