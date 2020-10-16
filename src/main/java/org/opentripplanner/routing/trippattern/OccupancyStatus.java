package org.opentripplanner.routing.trippattern;

public enum OccupancyStatus {

    UNKNOWN,
    /**
     * The vehicle is considered empty by most measures, and has few or no passengers onboard, but is
     * still accepting passengers.
     */
    EMPTY,
    /**
     * The vehicle has a large percentage of seats available. What percentage of free seats out of the
     * total seats available is to be considered large enough to fall into this category is determined
     * at the discretion of the producer.
     */
    MANY_SEATS_AVAILABLE,
    /**
     * The vehicle has a small percentage of seats available. What percentage of free seats out of the total seats available is to be considered small enough to fall into this category is determined at the discretion of the producer.
     */
    FEW_SEATS_AVAILABLE,
    /**
     * The vehicle can currently accommodate only standing passengers.
     */
    STANDING_ROOM_ONLY,
    /**
     * The vehicle can currently accommodate only standing passengers and has limited space for them.
     */
    CRUSHED_STANDING_ROOM_ONLY,
    /**
     * The vehicle is considered full by most measures, but may still be allowing passengers to board.
     */
    FULL,
    /**
     * The vehicle cannot accept passengers.
     */
    NOT_ACCEPTING_PASSENGERS
}
