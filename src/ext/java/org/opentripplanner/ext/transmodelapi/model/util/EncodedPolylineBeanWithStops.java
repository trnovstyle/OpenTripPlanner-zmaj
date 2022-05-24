package org.opentripplanner.ext.transmodelapi.model.util;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class EncodedPolylineBeanWithStops {
    private StopLocation fromQuay;
    private StopLocation toQuay;
    private EncodedPolylineBean pointsOnLink;


    public StopLocation getFromQuay() {
        return fromQuay;
    }

    public StopLocation getToQuay() {
        return toQuay;
    }

    public void setFromQuay(StopLocation fromQuay) {
        this.fromQuay = fromQuay;
    }

    public void setToQuay(StopLocation toQuay) {
        this.toQuay = toQuay;
    }

    public void setEncodedPolylineBean(EncodedPolylineBean encodedPolylineBean) {
        this.pointsOnLink = encodedPolylineBean;
    }

    public EncodedPolylineBean getEncodedPolylineBean() {
        return pointsOnLink;
    }
}
