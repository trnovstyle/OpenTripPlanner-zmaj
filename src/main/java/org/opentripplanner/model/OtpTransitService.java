package org.opentripplanner.model;

import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.List;

/**
 * Methods for accessing imported entities.
 */
public interface OtpTransitService {

    Collection<Agency> getAllAgencies();

    Collection<FareAttribute> getAllFareAttributes();

    Collection<FareRule> getAllFareRules();

    Collection<FeedInfo> getAllFeedInfos();

    Collection<Pathway> getAllPathways();

    /** @return all ids for both Calendars and CalendarDates merged into on list without duplicates */
    Collection<AgencyAndId> getAllServiceIds();

    Collection<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId);

    Stop getStopForId(AgencyAndId id);

    List<Stop> getStopsForStation(Stop station);

    Collection<Stop> getAllStops();

    /**
     * @return the list of {@link StopTime} objects associated with the trip,
     * sorted by {@link StopTime#getStopSequence()}
     */
    List<StopTime> getStopTimesForTrip(Trip trip);

    Collection<Transfer> getAllTransfers();

    Collection<TripPattern> getTripPatterns();

    Collection<Trip> getAllTrips();
}
