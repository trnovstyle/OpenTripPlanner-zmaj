package org.opentripplanner.graph_builder.triptransformer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TransitServiceDecorator {
    private final OtpTransitBuilder transitService;
    private final CalendarServiceData data;

    TransitServiceDecorator(OtpTransitBuilder transitService, CalendarServiceData data) {
        this.transitService = transitService;
        this.data = data;
    }

    Multimap<Route, Trip> tripsByRoute(ServiceDate date, Function<Trip, Boolean> filter, boolean allTrips) {
        Set<AgencyAndId> serviceIds = serviceIdsForDate(date);

        List<Trip> trips = findAllTripsByServiceIds(serviceIds);


        Set<Route> routes = new HashSet<>();
        Multimap<Route, Trip> tripsByRoute = ArrayListMultimap.create();

        for (Trip trip : trips) {
            if (filter.apply(trip)) {
                if(allTrips) {
                    routes.add(trip.getRoute());
                }
                else {
                    tripsByRoute.put(trip.getRoute(), trip);
                }
            }
        }
        if(allTrips) {
            for (Trip trip : trips) {
                if (routes.contains(trip.getRoute())) {
                    tripsByRoute.put(trip.getRoute(), trip);
                }
            }
        }
        return tripsByRoute;
    }

    List<Trip> findAllTrips(
            ServiceDate date,
            AgencyAndId routeId,
            Predicate<String> headsignFilter,
            IntPredicate departureTimeMinutesFilter
    ) {
        Set<AgencyAndId> serviceIds = serviceIdsForDate(date);
        Stream<Trip> tripStream = findAllTripsByServiceIds(serviceIds).stream()
                .filter(trip -> trip.getRoute().getId().equals(routeId));

        if(headsignFilter != null) {
            tripStream = tripStream.filter(it -> headsignFilter.test(it.getTripHeadsign()));
        }
        if(departureTimeMinutesFilter != null) {
            tripStream = tripStream.filter(it -> departureTimeMinutesFilter.test(departureTime(it)));
        }
        return tripStream.collect(Collectors.toList());
    }

        Trip findTrip(
            ServiceDate date,
            AgencyAndId routeId,
            Predicate<String> headsignFilter,
            IntPredicate departureTimeMinutesFilter
    ) {
        List<Trip> trips = findAllTrips(date, routeId, headsignFilter, departureTimeMinutesFilter);
        if(trips.size() != 1) throw new IllegalStateException(
                "Unexpected number of trips found for: date=" + date + ", route=" + routeId
                + ", result=" + trips + ", departureFilter=" + departureTimeMinutesFilter
                + ", headsignFilter=" + headsignFilter
        );
        return trips.get(0);
    }

    void copyTrip(Trip trip, int timeShiftSeconds, AgencyAndId targetServiceId) {
        AgencyAndId id = trip.getId();
        Trip newTrip = new Trip(trip);
        newTrip.setId(newId(id, timeShiftSeconds));

        List<StopTime> newTimes = new ArrayList<>();
        List<StopTime> times = getStopTimesForTrip(trip);

        for (StopTime it : times) {
            StopTime newTime = new StopTime(it);
            id = it.getId();
            newTime.setId(newId(id, timeShiftSeconds));
            newTime.setDepartureTime(calculateNewTime(it.getDepartureTime(), timeShiftSeconds));
            newTime.setArrivalTime(calculateNewTime(it.getArrivalTime(), timeShiftSeconds));
            newTimes.add(newTime);
        }
        newTrip.setServiceId(targetServiceId);

        addTrip(newTrip, newTimes);
    }

    void moveTrip(Trip trip, int timeShiftSeconds, AgencyAndId targetServiceId) {
        List<StopTime> newTimes = new ArrayList<>();
        List<StopTime> times = getStopTimesForTrip(trip);

        for (StopTime it : times) {
            StopTime newTime = new StopTime(it);
            newTime.setId(newId(it.getId(), timeShiftSeconds));
            newTime.setDepartureTime(calculateNewTime(it.getDepartureTime(), timeShiftSeconds));
            newTime.setArrivalTime(calculateNewTime(it.getArrivalTime(), timeShiftSeconds));
            newTimes.add(newTime);
        }
        trip.setServiceId(targetServiceId);

        transitService.getStopTimesSortedByTrip().replace(trip, newTimes);
    }

    boolean isOneDayService(Trip trip) {
        return data.getServiceDatesForServiceId(trip.getServiceId()).size() == 1;
    }

    int departureTime(Trip trip) {
        return transitService.getStopTimesSortedByTrip().get(trip).get(0).getDepartureTime();
    }

    boolean departAfter2400(Trip t) {
        return departureTime(t) > 24 * 3600;
    }

    boolean isDstSummerTime(Trip t) {
        return TimeUtil.isDstSummerTime(departureTime(t));
    }


    /* private methods */

    private List<Trip> findAllTripsByServiceIds(Collection<AgencyAndId> serviceIds) {
        List<Trip> trips = new ArrayList<>();
        for (Trip it : trips()) {
            if (serviceIds.contains(it.getServiceId())) {
                trips.add(it);
            }
        }
        return trips;
    }

    private List<StopTime> getStopTimesForTrip(Trip trip) {
        return transitService.getStopTimesSortedByTrip().get(trip);
    }

    private Set<AgencyAndId> serviceIdsForDate(ServiceDate date) {
        return data.getServiceIdsForDate(date);
    }

    private Collection<Trip> trips() {
        return transitService.getTrips().values();
    }

    private void addTrip(Trip newTrip, List<StopTime> newTimes) {
        transitService.getTrips().add(newTrip);
        transitService.getStopTimesSortedByTrip().put(newTrip, newTimes);
    }

    private AgencyAndId newId(AgencyAndId id, int suffix) {
        return new AgencyAndId(id.getAgencyId(), id.getId() + ":" + suffix);
    }

    private int calculateNewTime(int original, int delta) {
        if(original < 0) return original;
        int newTime = original + delta;
        if(newTime < 0) throw new IllegalArgumentException("Cant timeshift to a negative value.");
        return newTime;
    }
}
