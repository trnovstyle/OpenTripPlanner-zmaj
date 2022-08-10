package org.opentripplanner.ext.siri;

import static java.util.Collections.EMPTY_LIST;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.PickDrop.SCHEDULED;
import static org.opentripplanner.model.PickDrop.CANCELLED;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Supplier;
import javax.xml.datatype.Duration;

import org.opentripplanner.model.*;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.CallStatusEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.MonitoredCallStructure;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityStructure;

public class TimetableHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TimetableHelper.class);

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param journey  SIRI-ET EstimatedVehicleJourney
     * @param timeZone time zone of trip update
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static TripTimes createUpdatedTripTimes(final Graph graph, Timetable timetable, EstimatedVehicleJourney journey, TimeZone timeZone, FeedScopedId tripId) {
        if (journey == null) {
            return null;
        }

        int tripIndex = timetable.getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.debug("tripId {} not found in pattern.", tripId);
            return null;
        }

        final TripTimes existingTripTimes = timetable.getTripTimes(tripIndex);
        TripTimes oldTimes = new TripTimes(existingTripTimes);

        if (journey.isCancellation() != null && journey.isCancellation()) {
            oldTimes.cancelTrip();
            return oldTimes;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
        EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();

        List<EstimatedCall> estimatedCalls;
        if (journeyEstimatedCalls != null) {
            estimatedCalls = journeyEstimatedCalls.getEstimatedCalls();
        } else {
            estimatedCalls = EMPTY_LIST;
        }

        List<RecordedCall> recordedCalls;
        if (journeyRecordedCalls != null) {
            recordedCalls = journeyRecordedCalls.getRecordedCalls();
        } else {
            recordedCalls = EMPTY_LIST;
        }

        boolean stopPatternChanged = false;

        Trip trip = getTrip(tripId, timetable);

        List<StopTime> modifiedStopTimes = createModifiedStopTimes(timetable, oldTimes, journey, trip, new RoutingService(graph));
        if (modifiedStopTimes == null) {
            return null;
        }

        if (modifiedStopTimes.size() != (estimatedCalls.size() + recordedCalls.size())) {
            LOG.warn("Stop amount in ET message does not match with amount stops in trip.");
            return null;
        }

        TripTimes newTimes = new TripTimes(trip, modifiedStopTimes, graph.deduplicator);

        //Populate missing data from existing TripTimes
        newTimes.setServiceCode(oldTimes.getServiceCode());

        ZoneId zoneId = graph.getTimeZone().toZoneId();

        int callCounter = 0;
        final ZonedDateTime serviceDate = getServiceDate(journey, zoneId, oldTimes);
        Set<Object> alreadyVisited = new HashSet<>();

        boolean isJourneyPredictionInaccurate =  (journey.isPredictionInaccurate() != null && journey.isPredictionInaccurate());

        int departureFromPreviousStop = 0;
        int lastArrivalDelay = 0;
        int lastDepartureDelay = 0;
        for (var stop : timetable.getPattern().getStops()) {
            boolean foundMatch = false;

            for (RecordedCall recordedCall : recordedCalls) {
                if (alreadyVisited.contains(recordedCall)) {
                    continue;
                }
                //Current stop is being updated
                foundMatch = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

                if (!foundMatch && stop.isPartOfStation()) {
                    var alternativeStop = graph.index.getStopForId(
                            new FeedScopedId(stop.getId().getFeedId(), recordedCall.getStopPointRef().getValue())
                    );
                    if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                        foundMatch = true;
                        stopPatternChanged = true;
                    }
                }

                if (foundMatch) {

                    applyUpdates(
                            serviceDate,
                            modifiedStopTimes,
                            newTimes,
                            zoneId,
                            callCounter,
                            recordedCall
                    );

                    lastArrivalDelay = newTimes.getArrivalDelay(callCounter);
                    lastDepartureDelay = newTimes.getDepartureDelay(callCounter);
                    departureFromPreviousStop = newTimes.getDepartureTime(callCounter);

                    alreadyVisited.add(recordedCall);
                    break;
                }
            }
            if (!foundMatch) {
                for (EstimatedCall estimatedCall : estimatedCalls) {
                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    //Current stop is being updated
                    foundMatch = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!foundMatch && stop.isPartOfStation()) {
                        var alternativeStop = graph.index
                            .getStopForId(
                                    new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue())
                            );
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            foundMatch = true;
                            stopPatternChanged = true;
                        }
                    }

                    if (foundMatch) {

                        // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
                        boolean isCallPredictionInaccurate = estimatedCall.isPredictionInaccurate() != null && estimatedCall.isPredictionInaccurate();
                        newTimes.setPredictionInaccurate(callCounter, (isJourneyPredictionInaccurate | isCallPredictionInaccurate));

                        applyUpdates(
                                serviceDate,
                                modifiedStopTimes,
                                newTimes,
                                zoneId,
                                callCounter,
                                estimatedCall
                        );

                        lastArrivalDelay = newTimes.getArrivalDelay(callCounter);
                        lastDepartureDelay = newTimes.getDepartureDelay(callCounter);
                        departureFromPreviousStop = newTimes.getDepartureTime(callCounter);

                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }
            if (!foundMatch) {

                if (timetable.getPattern().isBoardAndAlightAt(callCounter, NONE)) {
                    // When newTimes contains stops without pickup/dropoff - set both arrival/departure to previous stop's departure
                    // This necessary to accommodate the case when delay is reduced/eliminated between to stops with pickup/dropoff, and
                    // multiple non-pickup/dropoff stops are in between.
                    newTimes.updateArrivalTime(callCounter, departureFromPreviousStop);
                    newTimes.updateDepartureTime(callCounter, departureFromPreviousStop);
                } else {

                    int arrivalDelay = lastArrivalDelay;
                    int departureDelay = lastDepartureDelay;

                    if (lastArrivalDelay == 0 && lastDepartureDelay == 0) {
                        //No match has been found yet (i.e. still in RecordedCalls) - keep existing delays
                        arrivalDelay = existingTripTimes.getArrivalDelay(callCounter);
                        departureDelay = existingTripTimes.getDepartureDelay(callCounter);
                    }

                    newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                    newTimes.updateDepartureDelay(callCounter, departureDelay);
                }

                departureFromPreviousStop = newTimes.getDepartureTime(callCounter);
            }
            callCounter++;
        }

        if (stopPatternChanged) {
            // This update modified stopPattern
            newTimes.setRealTimeState(RealTimeState.MODIFIED);
        } else {
            // This is the first update, and StopPattern has not been changed
            newTimes.setRealTimeState(RealTimeState.UPDATED);
        }

        if (journey.isCancellation() != null && journey.isCancellation()) {
            LOG.debug("Trip is cancelled");
            newTimes.cancelTrip();
        }

        if (!newTimes.timesIncreasing()) {
//            LOG.info("TripTimes are non-increasing after applying SIRI delay propagation - LineRef {}, TripId {}.", journey.getLineRef().getValue(), tripId);
            return null;
        }

        if (newTimes.getNumStops() != timetable.getPattern().numberOfStops()) {
            return null;
        }

        LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
        return newTimes;
    }

    private static ZonedDateTime getServiceDate(EstimatedVehicleJourney journey, ZoneId zoneId, TripTimes oldTimes){
        if (journey.getFramedVehicleJourneyRef() != null && journey.getFramedVehicleJourneyRef().getDataFrameRef() != null) {
            var dataFrame = journey.getFramedVehicleJourneyRef().getDataFrameRef();
            if (dataFrame != null) {
                try {
                    return LocalDate.parse(dataFrame.getValue()).atStartOfDay(zoneId);
                } catch (DateTimeParseException ignored) {
                    LOG.warn("Invalid dataFrame format: {}", dataFrame.getValue());
                }
            }
        }

        var recordedCalls = journey.getRecordedCalls();
        var estimatedCalls = journey.getEstimatedCalls();
        ZonedDateTime firstDeparture;
        if (recordedCalls.getRecordedCalls().isEmpty()) {
            firstDeparture = estimatedCalls.getEstimatedCalls().get(0).getAimedDepartureTime();
        } else {
            firstDeparture = recordedCalls.getRecordedCalls().get(0).getAimedDepartureTime();
        }

        return firstDeparture.minusDays(calculateDayOffset(oldTimes)).withZoneSameInstant(zoneId);
    }

    private static int calculateDayOffset(TripTimes oldTimes) {
        if (oldTimes.getDepartureTime(0) > 86400) {
            // The "departure-date" for this trip is set to "yesterday" (or before) even though it actually departs "today"

            return oldTimes.getDepartureTime(0)/86400; // calculate number of offset-days
        } else {
            return 0;
        }
    }

    /**
     * Loop through all passed times, return the first non-negative one or the last one
     */
    private static int handleMissingRealtime(int... times) {
        if (times.length == 0) {
            throw new IllegalArgumentException("Need at least one value");
        }

        int time = -1;
        for (int t : times) {
            time = t;
            if (time >= 0) {
                break;
            }
        }

        return time;
    }

    private static int getRealtimeTimeByPriority(ZoneId zoneId,
                                                 ZonedDateTime departureDate,
                                                 ZonedDateTime actualTime,
                                                 ZonedDateTime expectedTime,
                                                 ZonedDateTime aimedTime,
                                                 int defaultValue) {
        int realtimeArrivalTime;
        var time = getPrioritizedNullables(actualTime, expectedTime, aimedTime);
        realtimeArrivalTime = time.map(zonedDateTime -> DateMapper.secondsSinceStartOfService(departureDate, zonedDateTime, zoneId)).orElse(defaultValue);
        return realtimeArrivalTime;
    }

    private static Optional<ZonedDateTime> getPrioritizedNullables(ZonedDateTime actualTime, ZonedDateTime expectedTime, ZonedDateTime aimedTime) {
        return supplyNullable(actualTime).get()
                .or(supplyNullable(expectedTime))
                .or(supplyNullable(aimedTime));
    }

    private static <T> Supplier<Optional<T>> supplyNullable(T object) {
        return () -> Optional.ofNullable(object);
    }


    /**
     * Apply the SIRI ET to the appropriate TripTimes from this Timetable.
     * Calculate new stoppattern based on single stop cancellations
     *
     * @param journey    SIRI-ET EstimatedVehicleJourney
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static List<StopLocation> createModifiedStops(Timetable timetable, EstimatedVehicleJourney journey, RoutingService routingService) {
        if (journey == null) {
            return null;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
        EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();

        List<EstimatedCall> estimatedCalls;
        if (journeyEstimatedCalls != null) {
            estimatedCalls = journeyEstimatedCalls.getEstimatedCalls();
        } else {
            estimatedCalls = EMPTY_LIST;
        }

        List<RecordedCall> recordedCalls;
        if (journeyRecordedCalls != null) {
            recordedCalls = journeyRecordedCalls.getRecordedCalls();
        } else {
            recordedCalls = EMPTY_LIST;
        }

        //Get all scheduled stops
        var pattern = timetable.getPattern();

        // Keeping track of visited stop-objects to allow multiple visits to a stop.
        List<Object> alreadyVisited = new ArrayList<>();

        List<StopLocation> modifiedStops = new ArrayList<>();

        for (int i = 0; i < pattern.numberOfStops(); i++) {
            StopLocation stop = pattern.getStop(i);

            boolean foundMatch = false;
            if (i < recordedCalls.size()) {
                for (RecordedCall recordedCall : recordedCalls) {

                    if (alreadyVisited.contains(recordedCall)) {
                        continue;
                    }
                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        var alternativeStop = routingService
                            .getStopForId(new FeedScopedId(stop.getId().getFeedId(), recordedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stop = alternativeStop;
                        }
                    }

                    if (stopsMatchById) {
                        foundMatch = true;
                        modifiedStops.add(stop);
                        alreadyVisited.add(recordedCall);
                        break;
                    }
                }
            } else {
                for (EstimatedCall estimatedCall : estimatedCalls) {

                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        var alternativeStop = routingService
                            .getStopForId(new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stop = alternativeStop;
                        }
                    }

                    if (stopsMatchById) {
                        foundMatch = true;
                        modifiedStops.add(stop);
                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }
            if (!foundMatch) {
                modifiedStops.add(stop);
            }
        }


        return modifiedStops;
    }

    /**
     * Apply the SIRI ET to the appropriate TripTimes from this Timetable.
     * Calculate new stoppattern based on single stop cancellations
     *
     * @param journey    SIRI-ET EstimatedVehicleJourney
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static List<StopTime> createModifiedStopTimes(Timetable timetable, TripTimes oldTimes, EstimatedVehicleJourney journey, Trip trip, RoutingService routingService) {
        if (journey == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls;
        List<RecordedCall> recordedCalls;
        if (journey.getEstimatedCalls() != null) {
            estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();
        } else {
            estimatedCalls = EMPTY_LIST;
        }

        if (journey.getRecordedCalls() != null) {
            recordedCalls = journey.getRecordedCalls().getRecordedCalls();
        } else {
            recordedCalls = EMPTY_LIST;
        }

        var stops = createModifiedStops(timetable, journey, routingService);

        List<StopTime> modifiedStops = new ArrayList<>();

        ZonedDateTime departureDate = null;
        int numberOfRecordedCalls = (journey.getRecordedCalls() != null && journey.getRecordedCalls().getRecordedCalls() != null) ? journey.getRecordedCalls().getRecordedCalls().size() : 0;
        Set<Object> alreadyVisited = new HashSet<>();
        // modify updated stop-times
        for (int i = 0; i < stops.size(); i++) {
            StopLocation stop = stops.get(i);

            final StopTime stopTime = new StopTime();
            stopTime.setStop(stop);
            stopTime.setTrip(trip);
            stopTime.setStopSequence(i);
            stopTime.setDropOffType(timetable.getPattern().getAlightType(i));
            stopTime.setPickupType(timetable.getPattern().getBoardType(i));
            stopTime.setArrivalTime(oldTimes.getScheduledArrivalTime(i));
            stopTime.setDepartureTime(oldTimes.getScheduledDepartureTime(i));
            stopTime.setStopHeadsign(oldTimes.getHeadsign(i));
            stopTime.setHeadsignVias(oldTimes.getHeadsignVias(i));
            stopTime.setTimepoint(oldTimes.isTimepoint(i) ? 1 : 0);

            // TODO: Do we need to set the StopTime.id?
            //stopTime.setId(oldTimes.getStopTimeIdByIndex(i));

            boolean foundMatch = false;
            if (i < numberOfRecordedCalls) {
                for (RecordedCall recordedCall : recordedCalls) {
                    if (alreadyVisited.contains(recordedCall)) {
                        continue;
                    }

                    //Current stop is being updated
                    var callStopRef = recordedCall.getStopPointRef().getValue();
                    boolean stopsMatchById = stop.getId().getId().equals(callStopRef);

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        var alternativeStop = routingService.getStopForId(
                                new FeedScopedId(stop.getId().getFeedId(), callStopRef)
                        );
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stopTime.setStop(alternativeStop);
                        }

                    }

                    if (stopsMatchById) {
                        foundMatch = true;

                        if (recordedCall.isCancellation() != null && recordedCall.isCancellation()) {
                            stopTime.cancel();
                        }

                        modifiedStops.add(stopTime);
                        alreadyVisited.add(recordedCall);
                        break;
                    }
                }
            } else {
                for (EstimatedCall estimatedCall : estimatedCalls) {
                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    if (departureDate == null) {
                        departureDate = (estimatedCall.getAimedDepartureTime() != null ? estimatedCall.getAimedDepartureTime() : estimatedCall.getAimedArrivalTime());
                    }

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.isPartOfStation()) {
                        var alternativeStop = routingService
                            .getStopForId(new FeedScopedId(stop.getId().getFeedId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                            stopsMatchById = true;
                            stopTime.setStop(alternativeStop);
                        }

                    }

                    if (stopsMatchById) {
                        foundMatch = true;

                        CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
                        if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
                            stopTime.cancelDropOff();
                        } else {
                            var dropOffType = mapDropOffType(stopTime.getDropOffType(), estimatedCall.getArrivalBoardingActivity());
                            dropOffType.ifPresent(stopTime::setDropOffType);
                        }


                        CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
                        if (departureStatus == CallStatusEnumeration.CANCELLED) {
                            stopTime.cancelPickup();
                        } else {
                            var pickUpType = mapPickUpType(stopTime.getPickupType(), estimatedCall.getDepartureBoardingActivity());
                            pickUpType.ifPresent(stopTime::setPickupType);
                        }

                        if (estimatedCall.isCancellation() != null && estimatedCall.isCancellation()) {
                            stopTime.cancel();
                        }

                        if (estimatedCall.getDestinationDisplaies() != null && !estimatedCall.getDestinationDisplaies().isEmpty()) {
                            NaturalLanguageStringStructure destinationDisplay = estimatedCall.getDestinationDisplaies().get(0);
                            stopTime.setStopHeadsign(destinationDisplay.getValue());
                        }

                        modifiedStops.add(stopTime);
                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }

            if (!foundMatch) {
                modifiedStops.add(stopTime);
            }
        }

        return modifiedStops;
    }

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param activity SIRI-VM VehicleActivity
     * @param timeZone time zone of trip update
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public static TripTimes createUpdatedTripTimes(Timetable timetable, Graph graph, VehicleActivityStructure activity, TimeZone timeZone, FeedScopedId tripId) {
        if (activity == null) {
            return null;
        }

        MonitoredVehicleJourneyStructure mvj = activity.getMonitoredVehicleJourney();


        int tripIndex = timetable.getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.trace("tripId {} not found in pattern.", tripId);
            return null;
        }

        final TripTimes existingTripTimes = timetable.getTripTimes(tripIndex);
        TripTimes newTimes = new TripTimes(existingTripTimes);


        MonitoredCallStructure update = mvj.getMonitoredCall();
        if (update == null) {
            return null;
        }

        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();

        Duration delay = null;
        if (monitoredVehicleJourney != null) {
            delay = monitoredVehicleJourney.getDelay();
            int updatedDelay = 0;
            if (delay != null) {
                updatedDelay = delay.getSign() * (delay.getHours() * 3600 + delay.getMinutes() * 60 + delay.getSeconds());
            }

            MonitoredCallStructure monitoredCall = monitoredVehicleJourney.getMonitoredCall();
            if (monitoredCall != null && monitoredCall.getStopPointRef() != null) {
                boolean matchFound = false;

                int arrivalDelay = 0;
                int departureDelay = 0;
                var pattern = timetable.getPattern();

                for (int index = 0; index < newTimes.getNumStops(); ++index) {
                    if (!matchFound) {
                        // Delay is set on a single stop at a time. When match is found - propagate delay on all following stops
                        final var stop =  pattern.getStop(index);

                        matchFound = stop.getId().getId().equals(monitoredCall.getStopPointRef().getValue());

                        if (!matchFound && stop.isPartOfStation()) {
                            FeedScopedId alternativeId = new FeedScopedId(stop.getId().getFeedId(), monitoredCall.getStopPointRef().getValue());
                            var alternativeStop = graph.index.getStopForId(alternativeId);
                            if (alternativeStop != null && alternativeStop.isPartOfStation()) {
                                matchFound = stop.isPartOfSameStationAs(alternativeStop);
                            }
                        }


                        if (matchFound) {
                            arrivalDelay = departureDelay = updatedDelay;
                        } else {
                            /*
                             * If updated delay is less than previously set delay, the existing delay needs to be adjusted to avoid
                             * non-increasing times causing updates to be rejected. Will only affect historical data.
                             */
                            arrivalDelay = Math.min(existingTripTimes.getArrivalDelay(index), updatedDelay);
                            departureDelay =  Math.min(existingTripTimes.getDepartureDelay(index), updatedDelay);
                        }
                    }
                    newTimes.updateArrivalDelay(index, arrivalDelay);
                    newTimes.updateDepartureDelay(index, departureDelay);
                }
            }
        }

        if (!newTimes.timesIncreasing()) {
//            LOG.info("TripTimes are non-increasing after applying SIRI delay propagation - delay: {}", delay);
            return null;
        }

        //If state is already MODIFIED - keep existing state
        if (newTimes.getRealTimeState() != RealTimeState.MODIFIED) {
            // Make sure that updated trip times have the correct real time state
            newTimes.setRealTimeState(RealTimeState.UPDATED);
        }

        return newTimes;
    }

    /**
     * This method maps an ArrivalBoardingActivity to a pick drop type.
     *
     * The Siri ArrivalBoardingActivity includes less information than the pick drop type, therefore is it only
     * changed if routability has changed.
     *
     * @param currentValue The current pick drop value on a stopTime
     * @param arrivalBoardingActivityEnumeration The incoming boardingActivity to be mapped
     * @return Mapped PickDrop type, empty if routability is not changed.
     */
    public static Optional<PickDrop> mapDropOffType(PickDrop currentValue, ArrivalBoardingActivityEnumeration arrivalBoardingActivityEnumeration) {
        switch (arrivalBoardingActivityEnumeration) {
            case ALIGHTING:
                if (currentValue.isNotRoutable()) {
                    return Optional.of(SCHEDULED);
                }
                return Optional.empty();
            case NO_ALIGHTING:
                return Optional.of(NONE);
            case PASS_THRU:
                return Optional.of(CANCELLED);
            default:
                return Optional.empty();
        }
    }

    /**
     * This method maps an departureBoardingActivity to a pick drop type.
     *
     * The Siri DepartureBoardingActivity includes less information than the planned data, therefore is it only
     * changed if routability has changed.
     *
     * @param currentValue The current pick drop value on a stopTime
     * @param departureBoardingActivityEnumeration The incoming departureBoardingActivityEnumeration to be mapped
     * @return Mapped PickDrop type, empty if routability is not changed.
     */
    public static Optional<PickDrop> mapPickUpType(PickDrop currentValue, DepartureBoardingActivityEnumeration departureBoardingActivityEnumeration) {
        switch (departureBoardingActivityEnumeration) {
            case BOARDING:
                if (currentValue.isNotRoutable()) {
                    return Optional.of(SCHEDULED);
                }
                return Optional.empty();
            case NO_BOARDING:
                return Optional.of(NONE);
            case PASS_THRU:
                return Optional.of(CANCELLED);
            default:
                return Optional.empty();
        }
    }

    /**
     * @return the matching Trip in this particular Timetable
     */
    public static Trip getTrip(FeedScopedId tripId, Timetable timetable) {
        for (TripTimes tt : timetable.getTripTimes()) {
            if (tt.getTrip().getId().equals(tripId)) {
                return tt.getTrip();
            }
        }
        return null;
    }

    public static void applyUpdates(
            ZonedDateTime departureDate,
            List<StopTime> stopTimes,
            TripTimes tripTimes,
            ZoneId zoneId,
            int index,
            RecordedCall recordedCall
    ) {
        if (recordedCall.isCancellation() != null && recordedCall.isCancellation()) {
            stopTimes.get(index).cancel();
            tripTimes.setCancelled(index);
        }

        int arrivalTime = tripTimes.getArrivalTime(index);
        if (recordedCall.getActualArrivalTime() != null) {
            //Flag as recorded
            tripTimes.setRecorded(index, true);
        }

        int realtimeArrivalTime = getRealtimeTimeByPriority(
                zoneId,
                departureDate,
                recordedCall.getActualArrivalTime(),
                recordedCall.getExpectedArrivalTime(),
                recordedCall.getAimedArrivalTime(),
                -1
        );

        int departureTime = tripTimes.getDepartureTime(index);
        if (recordedCall.getActualDepartureTime() != null) {
            //Flag as recorded
            tripTimes.setRecorded(index, true);
        }

        int realtimeDepartureTime = getRealtimeTimeByPriority(
                zoneId,
                departureDate,
                recordedCall.getActualDepartureTime(),
                recordedCall.getExpectedDepartureTime(),
                recordedCall.getAimedDepartureTime(),
                -1
        );

        if (index == 0) {
            realtimeArrivalTime = handleMissingRealtime(realtimeArrivalTime, realtimeDepartureTime, arrivalTime);
        } else {
            realtimeArrivalTime = handleMissingRealtime(realtimeArrivalTime, arrivalTime);
        }

        if (index == (stopTimes.size() - 1)) {
            realtimeDepartureTime = handleMissingRealtime(realtimeDepartureTime, realtimeArrivalTime, departureTime);
        } else {
            realtimeDepartureTime = handleMissingRealtime(realtimeDepartureTime, departureTime);
        }

        int arrivalDelay = realtimeArrivalTime - arrivalTime;
        tripTimes.updateArrivalDelay(index, arrivalDelay);

        int departureDelay = realtimeDepartureTime - departureTime;
        tripTimes.updateDepartureDelay(index, departureDelay);
    }

    public static void applyUpdates(
            ZonedDateTime departureDate,
            List<StopTime> stopTimes,
            TripTimes tripTimes,
            ZoneId zoneId,
            int index,
            EstimatedCall estimatedCall
    ) {
        if (estimatedCall.isCancellation() != null && estimatedCall.isCancellation()) {
            stopTimes.get(index).cancel();
            tripTimes.setCancelled(index);
        }

        // Update dropoff-/pickuptype only if status is cancelled
        CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
        if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
            stopTimes.get(index).cancelDropOff();
        }

        CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
        if (departureStatus == CallStatusEnumeration.CANCELLED) {
            stopTimes.get(index).cancelPickup();
        }

        int arrivalTime = tripTimes.getArrivalTime(index);
        int realtimeArrivalTime = getRealtimeTimeByPriority(
                zoneId,
                departureDate,
                null,
                estimatedCall.getExpectedArrivalTime(),
                estimatedCall.getAimedArrivalTime(),
                -1
        );

        int departureTime = tripTimes.getDepartureTime(index);
        int realtimeDepartureTime = getRealtimeTimeByPriority(
                zoneId,
                departureDate,
                null,
                estimatedCall.getExpectedDepartureTime(),
                estimatedCall.getAimedDepartureTime(),
                -1
        );

        if (index == 0) {
            realtimeArrivalTime = handleMissingRealtime(realtimeArrivalTime, realtimeDepartureTime, arrivalTime);
        } else {
            realtimeArrivalTime = handleMissingRealtime(realtimeArrivalTime, arrivalTime);
        }

        if (index == (stopTimes.size() - 1)) {
            realtimeDepartureTime = handleMissingRealtime(realtimeDepartureTime, realtimeArrivalTime, departureTime);
        } else {
            realtimeDepartureTime = handleMissingRealtime(realtimeDepartureTime, departureTime);
        }

        int arrivalDelay = realtimeArrivalTime - arrivalTime;
        tripTimes.updateArrivalDelay(index, arrivalDelay);

        int departureDelay = realtimeDepartureTime - departureTime;
        tripTimes.updateDepartureDelay(index, departureDelay);
    }
}
