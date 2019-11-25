package org.opentripplanner.openstreetmap.impl;

import org.opentripplanner.standalone.datastore.DataSource;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

/**
 * @author Vincent Privat
 * @since 1.0
 */
public class StreamedFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private final DataSource dataSource;

    public StreamedFileBasedOpenStreetMapProviderImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            StreamedOpenStreetMapParser.parseMap(dataSource.asInputStream(), handler, 1);
            handler.doneFirstPhaseRelations();

            StreamedOpenStreetMapParser.parseMap(dataSource.asInputStream(), handler, 2);
            handler.doneSecondPhaseWays();

            StreamedOpenStreetMapParser.parseMap(dataSource.asInputStream(), handler, 3);
            handler.doneThirdPhaseNodes();
        } catch (Exception ex) {
            throw new RuntimeException(
                    "error loading OSM from path " + dataSource.path(), ex
            );
        }
    }

    public String toString() {
        return "StreamedFileBasedOpenStreetMapProviderImpl(" + dataSource.path() + ")";
    }

    @Override
    public void checkInputs() {
        if (!dataSource.exist()) {
            throw new RuntimeException("Can't read OSM path: " + dataSource.path());
        }
    }
}
