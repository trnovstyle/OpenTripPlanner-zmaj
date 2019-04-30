package org.opentripplanner.index.model;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class AreaShort {
    public AgencyAndId areaId;
    public EncodedPolylineBean polygon;

    public AreaShort(AgencyAndId areaId, Geometry polygon) {
        this.areaId = areaId;
        this.polygon = PolylineEncoder.createEncodings(polygon.getBoundary());
    }
}
