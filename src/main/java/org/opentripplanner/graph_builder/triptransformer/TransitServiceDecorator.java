package org.opentripplanner.graph_builder.triptransformer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;

import javax.annotation.Nonnull;
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

@SuppressWarnings("SameParameterValue")
class TransitServiceDecorator {
    private final OtpTransitBuilder transitService;
    private final MyCalService calService ;

    TransitServiceDecorator(OtpTransitBuilder transitService) {
        this.transitService = transitService;
        this.calService = new MyCalService(transitService.getCalendarDates());
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

    @Nonnull
    List<TripDeparture> findAllTrips(
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
        return tripStream.map(t -> new TripDeparture(t, departureTime(t))).collect(Collectors.toList());
    }

    @Nonnull
    TripDeparture findTrip(
            ServiceDate date,
            AgencyAndId routeId,
            Predicate<String> headsignFilter,
            IntPredicate departureTimeMinutesFilter
    ) {
        List<TripDeparture> trips = findAllTrips(date, routeId, headsignFilter, departureTimeMinutesFilter);
        if(trips.size() != 1) throw new IllegalStateException(
                "Unexpected number of trips found for: date=" + date + ", route=" + routeId
                + ", result=" + trips + ", departureFilter=" + departureTimeMinutesFilter
                + ", headsignFilter=" + headsignFilter
        );
        return trips.get(0);
    }

    @Nonnull
    List<TripDeparture> findAllTripsByPublicCode(ServiceDate date, String publicCode) {
        final Set<AgencyAndId> serviceIds = serviceIdsForDate(date);
        return trips().stream()
                .filter(t -> serviceIds.contains(t.getServiceId()))
                .filter(t -> t.getRoute().getShortName().equalsIgnoreCase(publicCode))
                .map(t -> new TripDeparture(t, departureTime(t)))
                .collect(Collectors.toList());
    }

    void copyTrip(TripDeparture tripDeparture, int timeShiftSeconds, ServiceDate date) {
        int newDepartureTime = tripDeparture.departureTime + timeShiftSeconds;
        Trip trip = tripDeparture.trip;

        if(newDepartureTime < 0) {
            // No use for so fare, can be implemented in the same way as the move method below
            throw new IllegalArgumentException("Can no copy trip to previous day. time: "
                    + TimeUtil.timeToString(newDepartureTime) + ", trip: " + trip);
        }

        MyCalService.NewServiceIds ids = calService.newServiceIds(trip.getServiceId(), date, date);

        copyTrip(trip, timeShiftSeconds, ids.targetId);
        trip.setServiceId(ids.otherDaysId);
    }

    void moveTrip(TripDeparture tripDeparture, int timeShiftSeconds, ServiceDate date) {
        int newDepartureTime = tripDeparture.departureTime + timeShiftSeconds;
        Trip trip = tripDeparture.trip;
        ServiceDate newDate = date;
        int newTimeShift = timeShiftSeconds;

        // Move trip to previous operation day
        if(newDepartureTime < 0) {
            newDate = TimeUtil.dayBefore(date);
            newTimeShift = timeShiftSeconds + TimeUtil.offsetPrevDaySec(date);
        }

        MyCalService.NewServiceIds ids = calService.newServiceIds(trip.getServiceId(), date, newDate);

        // Trip is running on move than one service day
        // Service needs to be split
        if(ids.otherDaysId != null) {
            // Copy trip onto new service and timeshift it
            copyTrip(trip, newTimeShift, ids.targetId);
            // Remove day from original trip
            trip.setServiceId(ids.otherDaysId);
        }
        else {
            moveTrip(trip, newTimeShift, ids.targetId);
        }
    }

    boolean isOneDayService(Trip trip) {
        return calService.isOnlyOneDatesForServiceId(trip.getServiceId());
    }

    int departureTime(Trip trip) {
        return transitService.getStopTimesSortedByTrip().get(trip).get(0).getDepartureTime();
    }

    boolean departAfter2400(Trip t) {
        return departureTime(t) > 24 * 3600;
    }

    boolean isBefore_04_30(Trip t) {
        return  departureTime(t) < (4 * 60 + 30) * 60;
    }


    /* private methods */

    private void copyTrip(Trip trip, int timeShiftSeconds, AgencyAndId targetServiceId) {
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

    private void moveTrip(Trip trip, int timeShiftSeconds, AgencyAndId targetServiceId) {
        List<StopTime> times = getStopTimesForTrip(trip);

        for (StopTime it : times) {
            it.setId(newId(it.getId(), timeShiftSeconds));
            it.setDepartureTime(calculateNewTime(it.getDepartureTime(), timeShiftSeconds));
            it.setArrivalTime(calculateNewTime(it.getArrivalTime(), timeShiftSeconds));
        }
        trip.setServiceId(targetServiceId);
    }

    @Deprecated
    AgencyAndId findServiceDefinedOnlyOn(ServiceDate serviceDate) {
        Set<AgencyAndId> serviceIds = serviceIdsForDate(serviceDate);
        for (AgencyAndId serviceId : serviceIds) {
            if(calService.isOnlyOneDatesForServiceId(serviceId)) {
                return serviceId;
            }
        }
        throw new IllegalStateException("There is no service defined for ONLY " + serviceDate + ".");
    }

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
        return calService.getServiceIdsForDate(date);
    }

    private Collection<Trip> trips() {
        return transitService.getTrips().values();
    }

    private void addTrip(Trip newTrip, List<StopTime> newTimes) {
        transitService.getTrips().add(newTrip);
        transitService.getStopTimesSortedByTrip().put(newTrip, newTimes);
    }

    private static AgencyAndId newId(AgencyAndId id, int suffix) {
        return new AgencyAndId(id.getAgencyId(), id.getId() + "-" + suffix);
    }

    private int calculateNewTime(int original, int delta) {
        if(original < 0) return original;
        int newTime = original + delta;
        if(newTime < 0) throw new IllegalArgumentException(
                "Cant timeshift to a negative value: " + TimeUtil.timeToString(newTime)
        );
        return newTime;
    }
}
