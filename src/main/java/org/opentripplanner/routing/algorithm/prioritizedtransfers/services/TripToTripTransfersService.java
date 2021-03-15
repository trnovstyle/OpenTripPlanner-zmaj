package org.opentripplanner.routing.algorithm.prioritizedtransfers.services;

import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findArrivalStopPosition;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findDepartureStopPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.model.StopTime;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.model.TripStopTime;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * TODO TGR
 *
 * @param <T>
 */
public class TripToTripTransfersService<T extends RaptorTripSchedule> {

  private final RaptorSlackProvider slackProvider;
  private final RaptorTransitDataProvider<T> stdTransfers;

  private T fromTrip;
  private T toTrip;


  public TripToTripTransfersService(
      RaptorSlackProvider slackProvider,
      RaptorTransitDataProvider<T> stdTransfers
  ) {
    this.slackProvider = slackProvider;
    this.stdTransfers = stdTransfers;
  }

  public List<TripToTripTransfer<T>> findTransfers(
      T fromTrip,
      StopTime fromTripDeparture,
      T toTrip
  ) {
    this.fromTrip = fromTrip;
    this.toTrip = toTrip;

    return findAllTransfers(fromTripDeparture);
  }

  private List<TripToTripTransfer<T>> findAllTransfers(StopTime fromTripDeparture) {

    final List<TripToTripTransfer<T>> result = new ArrayList<>();

    int stopPos = findArrivalStopPosition(fromTrip, fromTripDeparture.time(), fromTripDeparture.stop());

    while (stopPos < fromTrip.pattern().numberOfStopsInPattern()) {
      var from = TripStopTime.arrival(fromTrip, stopPos);

      // First add high priority transfers

      result.addAll(transferFromSameStop(from));
      result.addAll(findStandardTransfers(from));

      ++stopPos;
    }
    return result;
  }

  private Collection<? extends TripToTripTransfer<T>> findStandardTransfers(TripStopTime<T> from) {
    final List<TripToTripTransfer<T>> result = new ArrayList<>();
    Iterator<? extends RaptorTransfer> transfers = stdTransfers.getTransfers(from.stop());

    while (transfers.hasNext()) {
      var it = transfers.next();
      int toStop = it.stop();
      int earliestDepartureTime = earliestDepartureTime(from.time(), it.durationInSeconds());
      int toTripStopPos = findDepartureStopPosition(toTrip, earliestDepartureTime, toStop);

      if(toTripStopPos < 0) { continue; }

      var to = TripStopTime.departure(toTrip, toStop, toTripStopPos);

      result.add(new TripToTripTransfer<T>(from, to, it));
    }
    return result;
  }

  private Collection<TripToTripTransfer<T>> transferFromSameStop(TripStopTime<T> from) {
    final int stop = from.stop();
    final int earliestDepartureTime = earliestDepartureTime(from.time(),0);
    final int toTripStopPos = findDepartureStopPosition(toTrip, earliestDepartureTime, stop);

    if(toTripStopPos < 0) { return List.of(); }

    return List.of(
        new TripToTripTransfer<T>(from, TripStopTime.departure(toTrip, stop, toTripStopPos), null)
    );
  }

  private int earliestDepartureTime(int  fromTime, int transferDurationInSeconds) {
    return fromTime
        + slackProvider.transitSlack(fromTrip.pattern())
        + transferDurationInSeconds;
  }
}
