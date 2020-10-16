package org.opentripplanner.model;

public enum TripServiceAlteration {
    cancellation,
    planned,
    extraJourney,
    replaced;

    public boolean isCanceledOrReplaced() {
        return this == cancellation || this == replaced;
    }
}
