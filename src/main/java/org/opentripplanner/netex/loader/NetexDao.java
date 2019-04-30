package org.opentripplanner.netex.loader;

import org.opentripplanner.netex.loader.support.*;
import org.rutebanken.netex.model.*;

import java.util.HashSet;
import java.util.Set;

/**
 * This class holds indexes of Netex objects for lookup during
 * the NeTEx import.
 * <p>
 * A NeTEx import is grouped into several levels: <em>shard data</em>, <em>group shared data</em>,
 * and <em>singel files</em>. To discard objects not needed any more; this class support the
 * creation of multiple levels, by storing a refernece to a parent at an higher level. The
 * <code>HierarchicalMap.lookup</code> method first look in the local index, and then if nothing is found
 * delegate the lookup to its parent.
 * <p>
 */
public class NetexDao {

    public final HierarchicalMap<String, Authority> authoritiesByGroupOfLinesId;
    public final HierarchicalMapById<Authority> authoritiesById;
    public final HierarchicalMap<String, Authority> authoritiesByNetworkId;
    public final HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId;
    public final HierarchicalMap<String, Boolean> dayTypeAvailable;
    public final HierarchicalMapById<DayType> dayTypeById;
    public final HierarchicalMapById<DestinationDisplay> destinationDisplayById;
    public final HierarchicalMultimapById<FlexibleStopPlace> flexibleStopPlaceById;
    public final HierarchicalMap<String, String> flexibleStopPlaceIdByStopPointRef;
    public final HierarchicalMapById<GroupOfLines> groupOfLinesById;
    public final HierarchicalMap<String, GroupOfLines> groupOfLinesByLineId;
    public final HierarchicalMapById<GroupOfStopPlaces> groupsOfStopPlacesById;
    public final HierarchicalMap<String, ServiceJourneyInterchange> interchanges;
    public final HierarchicalMapById<JourneyPattern> journeyPatternsById;
    public final HierarchicalMap<String, JourneyPattern> journeyPatternsByStopPointId;
    public final HierarchicalMapById<Line_VersionStructure> lineById;
    public final HierarchicalMapById<StopPlace> multimodalStopPlaceById;
    public final HierarchicalMapById<Network> networkById;
    public final HierarchicalMap<String, Network> networkByLineId;
    public final HierarchicalMapById<NoticeAssignment> noticeAssignmentById;
    public final HierarchicalMapById<Notice> noticeById;
    public final HierarchicalMapById<Branding> brandingById;
    public final HierarchicalMapById<Route> routeById;
    public final HierarchicalMapById<OperatingPeriod> operatingPeriodById;
    public final HierarchicalMapById<Operator> operatorsById;
    public final HierarchicalMultimapById<Parking> parkingById;
    public final HierarchicalMultimapById<Quay> quayById;
    public final HierarchicalMap<String, String> quayIdByStopPointRef;
    public final HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId;
    public final HierarchicalMapById<ServiceLink> serviceLinkById;
    public final HierarchicalMultimapById<StopPlace> stopPlaceById;
    public final HierarchicalMapById<StopPointInJourneyPattern> stopPointInJourneyPatternById;
    public final HierarchicalMapById<TariffZone> tariffZoneById;

    private final Set<String> calendarServiceIds = new HashSet<>();

    private String timeZone = null;

    private final NetexDao parent;

    /**
     * Create a root node.
     */
    NetexDao() {
        this.parent = null;

        this.authoritiesByGroupOfLinesId = new HierarchicalMap<>();
        this.authoritiesById = new HierarchicalMapById<>();
        this.authoritiesByNetworkId = new HierarchicalMap<>();
        this.dayTypeAssignmentByDayTypeId = new HierarchicalMultimap<>();
        this.dayTypeAvailable = new HierarchicalMap<>();
        this.dayTypeById = new HierarchicalMapById<>();
        this.destinationDisplayById = new HierarchicalMapById<>();
        this.flexibleStopPlaceById = new HierarchicalMultimapById<>();
        this.flexibleStopPlaceIdByStopPointRef = new HierarchicalMap<>();
        this.groupOfLinesById = new HierarchicalMapById<>();
        this.groupOfLinesByLineId = new HierarchicalMap<>();
        this.groupsOfStopPlacesById = new HierarchicalMapById<>();
        this.interchanges = new HierarchicalMap<>();
        this.journeyPatternsById = new HierarchicalMapById<>();
        this.journeyPatternsByStopPointId = new HierarchicalMap<>();
        this.lineById = new HierarchicalMapById<>();
        this.multimodalStopPlaceById = new HierarchicalMapById<>();
        this.networkById = new HierarchicalMapById<>();
        this.networkByLineId = new HierarchicalMap<>();
        this.noticeAssignmentById = new HierarchicalMapById<>();
        this.noticeById = new HierarchicalMapById<>();
        this.brandingById = new HierarchicalMapById<>();
        this.operatingPeriodById = new HierarchicalMapById<>();
        this.operatorsById = new HierarchicalMapById<>();
        this.parkingById = new HierarchicalMultimapById<>();
        this.quayById = new HierarchicalMultimapById<>();
        this.quayIdByStopPointRef = new HierarchicalMap<>();
        this.routeById = new HierarchicalMapById<>();
        this.serviceJourneyByPatternId = new HierarchicalMultimap<>();
        this.serviceLinkById = new HierarchicalMapById<>();
        this.stopPlaceById = new HierarchicalMultimapById<>();
        this.stopPointInJourneyPatternById = new HierarchicalMapById<>();
        this.tariffZoneById = new HierarchicalMapById<>();
    }

    /**
     * Create a child node.
     * @param parent can not be <code>null</code>.
     */
    NetexDao(NetexDao parent) {
        this.parent = parent;

        this.authoritiesByGroupOfLinesId = new HierarchicalMap<>(parent.authoritiesByGroupOfLinesId);
        this.authoritiesById = new HierarchicalMapById<>(parent.authoritiesById);
        this.authoritiesByNetworkId = new HierarchicalMap<>(parent.authoritiesByNetworkId);
        this.dayTypeAssignmentByDayTypeId = new HierarchicalMultimap<>(parent.dayTypeAssignmentByDayTypeId);
        this.dayTypeAvailable = new HierarchicalMap<>(parent.dayTypeAvailable);
        this.dayTypeById = new HierarchicalMapById<>(parent.dayTypeById);
        this.destinationDisplayById = new HierarchicalMapById<>(parent.destinationDisplayById);
        this.flexibleStopPlaceById = new HierarchicalMultimapById<>(parent.flexibleStopPlaceById);
        this.flexibleStopPlaceIdByStopPointRef = new HierarchicalMap<>(parent.flexibleStopPlaceIdByStopPointRef);
        this.groupOfLinesById = new HierarchicalMapById<>(parent.groupOfLinesById);
        this.groupOfLinesByLineId = new HierarchicalMap<>(parent.groupOfLinesByLineId);
        this.groupsOfStopPlacesById = new HierarchicalMapById<>(parent.groupsOfStopPlacesById);
        // Interchanges are complex relations and the mapping is done AFTER all lines in a group is read
        this.interchanges = new DelegateToParentHierarchicalMap<>(parent.interchanges);
        this.journeyPatternsById = new HierarchicalMapById<>(parent.journeyPatternsById);
        this.journeyPatternsByStopPointId = new HierarchicalMap<>(parent.journeyPatternsByStopPointId);
        this.lineById = new HierarchicalMapById<>(parent.lineById);
        this.multimodalStopPlaceById = new HierarchicalMapById<>(parent.multimodalStopPlaceById);
        this.networkById = new HierarchicalMapById<>(parent.networkById);
        this.networkByLineId = new HierarchicalMap<>(parent.networkByLineId);
        this.noticeAssignmentById = new HierarchicalMapById<>(parent.noticeAssignmentById);
        this.noticeById = new HierarchicalMapById<>(parent.noticeById);
        this.brandingById = new HierarchicalMapById<>(parent.brandingById);
        this.operatingPeriodById = new HierarchicalMapById<>(parent.operatingPeriodById);
        this.operatorsById = new HierarchicalMapById<>(parent.operatorsById);
        this.parkingById = new HierarchicalMultimapById<>(parent.parkingById);
        this.quayById = new HierarchicalMultimapById<>(parent.quayById);
        // QuayIdByStopPointRefs are used for mapping Interchanges; hence must be available AFTER all lines are read
        this.quayIdByStopPointRef = new DelegateToParentHierarchicalMap<>(parent.quayIdByStopPointRef);
        this.routeById = new HierarchicalMapById<>(parent.routeById);
        this.serviceLinkById = new HierarchicalMapById<>(parent.serviceLinkById);
        this.serviceJourneyByPatternId = new HierarchicalMultimap<>(parent.serviceJourneyByPatternId);
        this.stopPlaceById = new HierarchicalMultimapById<>(parent.stopPlaceById);
        this.stopPointInJourneyPatternById = new HierarchicalMapById<>(parent.stopPointInJourneyPatternById);
        this.tariffZoneById = new HierarchicalMapById<>(parent.tariffZoneById);
    }

    public void addCalendarServiceId(String serviceId) {
        calendarServiceIds.add(serviceId);
    }

    public Iterable<String> getCalendarServiceIds() {
        return calendarServiceIds;
    }

    /**
     * Retrive timezone from this class, if not found delegate up to the parent NetexDao.
     */
    public String getTimeZone() {
        return (timeZone != null || parent == null) ? timeZone : parent.getTimeZone();
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

}
