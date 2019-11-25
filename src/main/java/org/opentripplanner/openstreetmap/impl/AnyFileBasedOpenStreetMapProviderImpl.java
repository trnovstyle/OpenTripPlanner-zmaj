/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.openstreetmap.impl;

import java.io.File;

import org.opentripplanner.standalone.datastore.FileType;
import org.opentripplanner.standalone.datastore.file.FileDataSource;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

public class AnyFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File path;

    public void setPath(File path) {
        this.path = path;
    }

    public AnyFileBasedOpenStreetMapProviderImpl (File file) {
        this.setPath(file);
    }
    
    public AnyFileBasedOpenStreetMapProviderImpl() { };

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            if (path.getName().endsWith(".pbf")) {
                BinaryFileBasedOpenStreetMapProviderImpl p = new BinaryFileBasedOpenStreetMapProviderImpl(
                        new FileDataSource(path, FileType.OSM)
                );
                p.readOSM(handler);
            } else {
                StreamedFileBasedOpenStreetMapProviderImpl p = new StreamedFileBasedOpenStreetMapProviderImpl(
                        new FileDataSource(path, FileType.OSM)
                );
                p.readOSM(handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + path, ex);
        }
    }

    public String toString() {
        return "AnyFileBasedOpenStreetMapProviderImpl(" + path + ")";
    }

    @Override
    public void checkInputs() {
        if (!path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + path);
        }
    }
}
