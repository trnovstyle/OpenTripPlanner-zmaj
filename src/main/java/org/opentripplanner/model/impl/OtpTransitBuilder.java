/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation; either version 3 of
 the License; or (at your option) any later version.

 This program is distributed in the hope that it will be useful;
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not; see <http://www.gnu.org/licenses/>. 
*/
package org.opentripplanner.model.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.IdentityBean;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TariffZone;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.model.Parking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OtpTransitBuilder {
    private final List<Agency> agencies = new ArrayList<>();


    private final List<ServiceCalendarDate> calendarDates = new ArrayList<>();

    private final List<ServiceCalendar> calendars = new ArrayList<>();

    private final List<FareAttribute> fareAttributes = new ArrayList<>();

    private final List<FareRule> fareRules = new ArrayList<>();

    private final List<FeedInfo> feedInfos = new ArrayList<>();

    private final List<Frequency> frequencies = new ArrayList<>();

    private final EntityMap<AgencyAndId, Stop> groupsOfStopPlaces = new EntityMap<>();

    private final ListMultimap<Stop, Stop> stopByGroupOfStopPlaces = ArrayListMultimap.create();

    private final List<Parking> parkings = new ArrayList<>();

    private final EntityMap<AgencyAndId, Stop> multiModalStops = new EntityMap<>();

    private final ListMultimap<Stop, Stop> stationsByMultiModalStop = ArrayListMultimap.create();

    private final EntityMap<AgencyAndId, Notice> noticesById = new EntityMap<>();

    private final EntityMap<AgencyAndId, NoticeAssignment> noticeAssignmentsById = new EntityMap<>();

    private final EntityMap<AgencyAndId, Operator> operatorsById = new EntityMap<>();

    private final List<Pathway> pathways = new ArrayList<>();

    private final EntityMap<AgencyAndId, Route> routesById = new EntityMap<>();

    private final List<ShapePoint> shapePoints = new ArrayList<>();

    private final EntityMap<AgencyAndId, Stop> stopsById = new EntityMap<>();

    private final SortedMultimap<Trip, StopTime> stopTimesByTrip = new SortedMultimap<>();

    private final EntityMap<AgencyAndId, TariffZone> tariffZonesById = new EntityMap<>();

    private final List<Transfer> transfers = new ArrayList<>();

    private final EntityMap<AgencyAndId, Trip> trips = new EntityMap<>();

    private final ListMultimap<StopPattern, TripPattern> tripPatterns = ArrayListMultimap.create();


    /* Accessors */

    public List<Agency> getAgencies() {
        return agencies;
    }

    public Agency findAgencyById(String id) {
        return agencies.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    public List<ServiceCalendarDate> getCalendarDates() {
        return calendarDates;
    }

    public List<ServiceCalendar> getCalendars() {
        return calendars;
    }

    public List<FareAttribute> getFareAttributes() {
        return fareAttributes;
    }

    public List<FareRule> getFareRules() {
        return fareRules;
    }

    public List<FeedInfo> getFeedInfos() {
        return feedInfos;
    }

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public EntityMap<AgencyAndId, Stop> getGroupsOfStopPlaces() {
        return groupsOfStopPlaces;
    }

    public ListMultimap<Stop, Stop> getStopByGroupOfStopPlaces() {
        return stopByGroupOfStopPlaces;
    }

    public EntityMap<AgencyAndId, Stop> getMultiModalStops() {
        return multiModalStops;
    }

    public EntityMap<AgencyAndId, Notice> getNoticesById() {
        return noticesById;
    }

    public EntityMap<AgencyAndId, NoticeAssignment> getNoticeAssignmentsById() {
        return noticeAssignmentsById;
    }

    public EntityMap<AgencyAndId, Operator> getOperatorsById() {
        return operatorsById;
    }

    public List<Parking> getParkings() { return parkings; }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public EntityMap<AgencyAndId, Route> getRoutes() {
        return routesById;
    }

    public List<ShapePoint> getShapePoints() {
        return shapePoints;
    }

    public ListMultimap<Stop, Stop> getStationsByMultiModalStop() {
        return stationsByMultiModalStop;
    }

    public EntityMap<AgencyAndId, Stop> getStops() {
        return stopsById;
    }

    public SortedMultimap<Trip, StopTime> getStopTimesSortedByTrip() {
        return stopTimesByTrip;
    }

    public EntityMap<AgencyAndId, TariffZone> getTariffZones() {
        return tariffZonesById;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public EntityMap<AgencyAndId, Trip> getTrips() {
        return trips;
    }

    public Multimap<StopPattern, TripPattern> getTripPatterns() {
        return tripPatterns;
    }

    /**
     * Find all serviceIds in both CalendarServices and CalendarServiceDates.
     */
    Set<AgencyAndId> findAllServiceIds() {
        Set<AgencyAndId> serviceIds = new HashSet<>();
        for (ServiceCalendar calendar : getCalendars()) {
            serviceIds.add(calendar.getServiceId());
        }
        for (ServiceCalendarDate date : getCalendarDates()) {
            serviceIds.add(date.getServiceId());
        }
        return serviceIds;
    }

    public OtpTransitService build() {
        createNoneExistingIds();

        return new OtpTransitServiceImpl(this);
    }

    private void createNoneExistingIds() {
        generateNoneExistingIds(feedInfos);
    }

    static <T extends IdentityBean<Integer>> void generateNoneExistingIds(Collection<T> entities) {
        int maxId = 0;
        for (T it : entities) {
            maxId = zeroOrNull(it.getId()) ? maxId : Math.max(maxId, it.getId());
        }
        for (T it : entities) {
            if(zeroOrNull(it.getId())) {
                it.setId(++maxId);
            }
        }
    }

    private static boolean zeroOrNull(Integer id) {
        return id == null || id == 0;
    }

    public void regenerateIndexes() {
        trips.reindex();
        this.stopsById.reindex();
        this.routesById.reindex();
        this.stopTimesByTrip.reindex();
    }
}
