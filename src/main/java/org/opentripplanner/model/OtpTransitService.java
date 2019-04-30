package org.opentripplanner.model;

import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Methods for accessing imported entities.
 */
public interface OtpTransitService {

    /**
     * Return a list of Agencies (NeTEx Authorities)
     */
    Collection<Agency> getAllAgencies();

    Collection<Area> getAllAreas();

    /**
     * NeTEx Operator. Not applicable for GTFS.
     */
    Collection<Operator> getAllOperators();

    Collection<FareAttribute> getAllFareAttributes();

    Collection<FareRule> getAllFareRules();

    Collection<FeedInfo> getAllFeedInfos();

    Collection<Parking> getAllParkings();

    Map<AgencyAndId, NoticeAssignment> getNoticeAssignmentById();

    Map<AgencyAndId, Notice> getNoticeById();

    Map<AgencyAndId, Branding> getBrandingById();

    Collection<Pathway> getAllPathways();

    /** @return all ids for both Calendars and CalendarDates merged into on list without duplicates */
    Collection<AgencyAndId> getAllServiceIds();

    Collection<ShapePoint> getShapePointsForShapeId(AgencyAndId shapeId);

    Stop getStopForId(AgencyAndId id);

    Map<Stop, Collection<Stop>> getStationsByMultiModalStop();

    Iterable<Map.Entry<Stop, Collection<Stop>>> getStopsByGroupOfStopPlace();

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
