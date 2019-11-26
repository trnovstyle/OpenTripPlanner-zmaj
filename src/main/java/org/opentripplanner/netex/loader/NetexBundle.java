package org.opentripplanner.netex.loader;


import org.opentripplanner.standalone.config.GraphBuilderParameters;
import org.opentripplanner.standalone.config.NetexParameters;
import org.opentripplanner.standalone.datastore.CompositeDataSource;

import java.io.Closeable;
import java.io.IOException;

public class NetexBundle implements Closeable {

    private final static double MAX_STOP_TO_SHAPE_SNAP_DISTANCE = 150;

    private final CompositeDataSource source;

    public final boolean linkStopsToParentStations;

    public final boolean linkMultiModalStopsToParentStations;

    public boolean parentStationTransfers;

    public final int subwayAccessTime;

    public final int maxInterlineDistance;

    public final NetexParameters netexParameters;

    public final boolean parkAndRideFromTransitData;

    public final boolean removeStopsNotInUse = true;

    public NetexBundle(CompositeDataSource source, GraphBuilderParameters builderParams) {
        this.source = source;
        this.linkStopsToParentStations = builderParams.parentStopLinking;
        this.parentStationTransfers = builderParams.stationTransfers;
        this.linkMultiModalStopsToParentStations = builderParams.linkMultiModalStopsToParentStations;
        this.subwayAccessTime = (int)(builderParams.subwayAccessTime * 60);
        this.maxInterlineDistance = builderParams.maxInterlineDistance;
        this.netexParameters = builderParams.netex;
        this.parkAndRideFromTransitData = builderParams.parkAndRideFromTransitData;
    }

    public String getFilename() {
        return source.path();
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    NetexDataSourceHierarchy fileHierarchy(){
        try {
            return new NetexDataSourceHierarchy(source, netexParameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkInputs() {
        if (!source.exists()) {
            throw new RuntimeException("NETEX Path " + source.path() + " does not exist.");
        }
    }

    public double getMaxStopToShapeSnapDistance() {
        return MAX_STOP_TO_SHAPE_SNAP_DISTANCE;
    }
}
