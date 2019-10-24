package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.model.Trip;

class Departure implements Comparable<Departure> {
    final Trip trip;
    final int time;
    final boolean oneDayService;

    Departure(Trip trip, int time, boolean oneDayService) {
        this.trip = trip;
        this.time = time;
        this.oneDayService = oneDayService;
    }

    String timeToString() {
        return TimeUtil.dstToString(time);
    }

    boolean isSummerTime() {
        return TimeUtil.isDstSummerTime(time);
    }

    @Override
    public int compareTo(Departure o) {
        return time - o.time;
    }

    @Override
    public String toString() {
        return timeToString()
                + " " + trip.getRoute().getShortName()
                + " " + trip.getServiceId();
    }
}
