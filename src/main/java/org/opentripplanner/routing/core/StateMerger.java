package org.opentripplanner.routing.core;

/**
 * Merge state data from multiple states before joining them.
 * <p>
 * This is dirty but necessary to achieve temporary fix for joining paths without to much interference in existing code.
 */
public class StateMerger {


    public static StateData merge(StateData transitState, StateData nonTransitState) {

        StateData merged = nonTransitState.clone();

        merged.tripTimes = transitState.tripTimes;
        merged.tripId = transitState.tripId;
        merged.everBoarded = transitState.everBoarded;
        merged.numBoardings = transitState.numBoardings;
        merged.previousTrip = transitState.previousTrip;
        merged.lastAlightedTime = transitState.lastAlightedTime;
        merged.lastPattern = transitState.lastPattern;
        merged.zone = transitState.zone;
        merged.route = transitState.route;
        merged.previousStop = transitState.previousStop;
        merged.routeSequence = transitState.routeSequence;
        merged.serviceDay = transitState.serviceDay;
        return merged;
    }


}
