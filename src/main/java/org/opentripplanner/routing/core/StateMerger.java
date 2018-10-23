package org.opentripplanner.routing.core;

/**
 * Merge state data from multiple states before joining them.
 * <p>
 * This is dirty but necessary to achieve temporary fix for joining paths without to much interference in existing code.
 */
public class StateMerger {


    public static StateData merge(StateData firstState, StateData nextState) {

        StateData merged = nextState.clone();

        if (firstState.everBoarded) {
            merged.tripTimes = firstState.tripTimes;
            merged.tripId = firstState.tripId;
            merged.everBoarded = firstState.everBoarded;
            merged.numBoardings = firstState.numBoardings;
            merged.previousTrip = firstState.previousTrip;
            merged.lastAlightedTime = firstState.lastAlightedTime;
            merged.lastPattern = firstState.lastPattern;
            merged.zone = firstState.zone;
            merged.route = firstState.route;
            merged.previousStop = firstState.previousStop;
            merged.routeSequence = firstState.routeSequence;
            merged.serviceDay = firstState.serviceDay;
        }
        return merged;
    }


}
