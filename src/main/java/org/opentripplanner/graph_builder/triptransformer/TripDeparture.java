package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;


/**
 * Wrap trip and first departure time
 */
class TripDeparture implements Comparable<TripDeparture> {
    final Trip trip;
    final int departureTime;

    TripDeparture(Trip trip, int departureTime) {
        this.trip = trip;
        this.departureTime = departureTime;
    }

    String departureTimeAsString() {
        return TimeUtil.timeToString(departureTime);
    }

    String id() {
        return String.format(
                "%s-%s-%s-%s",
                departureTimeAsString(),
                trip.getRoute().getId().getId(),
                trip.getTripHeadsign(),
                trip.getServiceId()
        );
    }

    @Override
    public int compareTo(TripDeparture o) {
        return departureTime - o.departureTime;
    }

    @Override
    public String toString() {
        Route route = trip.getRoute();
        return "<" + route.getShortName()+ " " + route.getId().getId() + " " + trip.getTripHeadsign()
                + " " + TimeUtil.timeToString(departureTime) + '>';
    }
}
