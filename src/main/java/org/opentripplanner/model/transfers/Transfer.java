/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model.transfers;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.base.ToStringBuilder;

public final class Transfer implements Serializable {
    private static final int NOT_SET = -1;

    private static final long serialVersionUID = 1L;

    private final Stop fromStop;

    private final Route fromRoute;

    private final Trip fromTrip;

    private final Stop toStop;

    private final Route toRoute;

    private final Trip toTrip;

    private final TransferPriority priority;

    private final boolean staySeated;

    private final boolean guaranteed;

    private final int minTransferTimeSeconds;

    private final int specificityRanking;

    public Transfer(Transfer obj) {
        this.fromStop = obj.fromStop;
        this.fromRoute = obj.fromRoute;
        this.fromTrip = obj.fromTrip;
        this.toStop = obj.toStop;
        this.toRoute = obj.toRoute;
        this.toTrip = obj.toTrip;
        this.staySeated = obj.staySeated;
        this.guaranteed = obj.guaranteed;
        this.priority = obj.priority;
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
            boolean staySeated,
            boolean guaranteed,
            TransferPriority priority,
            int minTransferTimeSeconds
    ) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.fromRoute = fromRoute;
        this.toRoute = toRoute;
        this.fromTrip = fromTrip;
        this.toTrip = toTrip;
        this.staySeated = staySeated;
        this.guaranteed = guaranteed;
        this.priority = priority;
        this.minTransferTimeSeconds = minTransferTimeSeconds;
        this.specificityRanking = calcSpecificityRanking(fromRoute, fromTrip)
            + calcSpecificityRanking(toRoute, toTrip);
    }

    public Transfer(
        Stop fromStop,
        Stop toStop,
        Trip fromTrip,
        Trip toTrip,
        boolean staySeated,
        boolean guaranteed,
        TransferPriority priority
    ) {
        this(
            fromStop, toStop,
            null, null,
            fromTrip, toTrip,
            staySeated,
            guaranteed,
            priority,
            NOT_SET
        );
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

    public TransferPriority getPriority() {
        return priority;
    }

    public boolean isStaySeated() {
        return staySeated;
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    public int getMinTransferTimeSeconds() {
        return minTransferTimeSeconds;
    }

    /**
     * <a href="https://developers.google.com/transit/gtfs/reference/gtfs-extensions#specificity-of-a-transfer">
     *   Specificity of a transfer
     * </a>
     */
    public int getSpecificityRanking() {
        return specificityRanking;
    }

    public String toString() {
        return ToStringBuilder.of(Transfer.class)
            .addObj("from", toString(fromStop, fromTrip, fromRoute))
            .addObj("to", toString(toStop, toTrip, toRoute))
            .addBoolIfTrue("staySeated", staySeated)
            .addBoolIfTrue("guaranteed", guaranteed)
            .toString();
    }

    private static int calcSpecificityRanking(Route route, Trip trip) {
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

    private String toString(Stop stop, Trip trip, Route route) {
        var t = trip == null ? route : trip;
        return t == null ? stop.toString() : "(" + stop + ", " + t + ")";
    }
}
