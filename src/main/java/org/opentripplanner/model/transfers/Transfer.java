/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfers;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

public final class Transfer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Stop fromStop;

    private final Route fromRoute;

    private final Trip fromTrip;

    private final Stop toStop;

    private final Route toRoute;

    private final Trip toTrip;

    private final TransferType transferType;

    private final int minTransferTimeSeconds;

    private final int specificityRanking;

    public Transfer(Transfer obj) {
        this.fromStop = obj.fromStop;
        this.fromRoute = obj.fromRoute;
        this.fromTrip = obj.fromTrip;
        this.toStop = obj.toStop;
        this.toRoute = obj.toRoute;
        this.toTrip = obj.toTrip;
        this.transferType = obj.transferType;
        this.minTransferTimeSeconds = obj.minTransferTimeSeconds;
        this.specificityRanking = obj.specificityRanking;
    }

    public Transfer(
            Stop fromStop,
            Stop toStop,
            Route fromRoute,
            Route toRoute,
            Trip fromTrip,
            Trip toTrip,
            TransferType transferType,
            int minTransferTimeSeconds
    ) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.fromRoute = fromRoute;
        this.toRoute = toRoute;
        this.fromTrip = fromTrip;
        this.toTrip = toTrip;
        this.transferType = transferType;
        this.minTransferTimeSeconds = minTransferTimeSeconds;
        this.specificityRanking = calcRanking(fromRoute, fromTrip) + calcRanking(toRoute, toTrip);
    }

    public boolean matches(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip
    ) {
        return matches(fromStop, fromTrip, this.fromStop, this.fromRoute, this.fromTrip)
            && matches(toStop, toTrip, this.toStop, this.toRoute, this.toTrip);
    }

    public Stop getFromStop() {
        return fromStop;
    }

    public Route getFromRoute() {
        return fromRoute;
    }

    public Trip getFromTrip() {
        return fromTrip;
    }

    public Stop getToStop() {
        return toStop;
    }

    public Route getToRoute() {
        return toRoute;
    }

    public Trip getToTrip() {
        return toTrip;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public int getMinTransferTimeSeconds() {
        return minTransferTimeSeconds;
    }

    public int getSpecificityRanking() {
        return specificityRanking;
    }

    public String toString() {
        return "<Transfer"
                + toStrOpt(" stop", fromStop, toStop)
                + toStrOpt(" route", fromRoute, toRoute)
                + toStrOpt(" trip", fromTrip, toTrip)
                + ">";
    }

    private static String toStrOpt(String lbl, TransitEntity arg1, TransitEntity arg2) {
        if(arg1 == null && arg2 == null) { return ""; }
        var buf = new StringBuilder(lbl).append("(");
        if(arg1 != null) { buf.append(arg1.getId()); }
        buf.append(" ~ ");
        if(arg2 != null) { buf.append(arg2.getId()); }
        return buf.append(")").toString();
    }

    private static int calcRanking(Route route, Trip trip) {
        if(trip != null) { return 2; }
        if(route != null) { return 1; }
        return 0;
    }

    /**
     * Do the given input (stop s, trip t) match the current Transfer (s0, r0, t0)
     */
    private static boolean matches(
        @Nonnull Stop s, @Nonnull Trip t,
        @Nonnull Stop s0, @Nullable Route r0, @Nullable Trip t0
    ) {
        if(s != s0) { return false; }

        if(t0 != null) {
            return t0 == t;
        }
        else if(r0 != null) {
            return r0 == t.getRoute();
        }
        return true;
    }
}
