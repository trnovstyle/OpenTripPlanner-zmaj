package org.opentripplanner.netex.mapping;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import javax.annotation.Nullable;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.transfers.Transfer;
import org.opentripplanner.model.transfers.TransferPriority;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferMapper {
  private static final Logger LOG = LoggerFactory.getLogger(TransferMapper.class);

  private final FeedScopedIdFactory idFactory;
  private final ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef;
  private final EntityById<Stop> stops;
  private final EntityById<Trip> trips;

  public TransferMapper(
      FeedScopedIdFactory idFactory,
      ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef,
      EntityById<Stop> stops,
      EntityById<Trip> trips
  ) {
    this.idFactory = idFactory;
    this.quayIdByStopPointRef = quayIdByStopPointRef;
    this.trips = trips;
    this.stops = stops;
  }

  /**
   * NeTEx ServiceJourneyInterchange example:
   * <pre>
   * ServiceJourneyInterchange {
   *    id: "VYG:ServiceJourneyInterchange:3"
   *    planned: true
   *    guaranteed: true
   *    fromPointRef.ref: "VYG:ScheduledStopPoint:VOS-BUS-341"
   *    toPointRef.ref: "VYG:ScheduledStopPoint:VOS-2"
   *    fromJourneyRef.ref: "VYG:ServiceJourney:BUS-610-322"
   *    toJourneyRef.ref: "VYG:ServiceJourney:610-323"
   * }
   * </pre>
   */
  @Nullable
  public Transfer mapToTransfer(ServiceJourneyInterchange it) {
    try {
      var id = it.getId();
      var fromStop = findStop("fromPointRef", id, it.getFromPointRef());
      var toStop = findStop("toPointRef", id, it.getToPointRef());
      var fromTrip = findTrip("fromJourneyRef", id, it.getFromJourneyRef());
      var toTrip = findTrip("toJourneyRef", id, it.getToJourneyRef());
      var staySeated = isTrue(it.isStaySeated());
      var guaranteed = isTrue(it.isGuaranteed());
      var priority = mapPriority(it.getPriority());

      return new Transfer(fromStop, toStop, fromTrip, toTrip, staySeated, guaranteed, priority);
    }
    catch (ExitMappingException e) {
      return null;
    }
  }

  private Stop findStop(String fieldName, String rootId, ScheduledStopPointRefStructure ref) {
    var quayId =  quayIdByStopPointRef.lookup(ref.getRef());
    assertRefExist("quayId", fieldName, rootId, ref.getRef(), quayId);
    var stopId = createId(fieldName, rootId, quayId);
    Stop stop = stops.get(stopId);
    assertRefExist("quay", fieldName, rootId, quayId, stop);
    return stop;
  }


  private Trip findTrip(String fieldName, String rootId, VehicleJourneyRefStructure ref) {
    var tripId = createId(fieldName, rootId, ref.getRef());
    Trip trip = trips.get(tripId);
    assertRefExist("Trip", fieldName, rootId, ref.getRef(), trip);
    return trip;
  }

  private TransferPriority mapPriority(Number pri) {
    if(pri == null) { return TransferPriority.ALLOWED;}
    switch (pri.intValue()) {
      case -1: return TransferPriority.NOT_ALLOWED;
      case 0: return TransferPriority.ALLOWED;
      case 1: return TransferPriority.RECOMMENDED;
      case 2: return TransferPriority.PREFERRED;
      default: throw new IllegalArgumentException("Interchange priority unknown: " + pri);
    }
  }

  private FeedScopedId createId(String fieldName, String rootId, String id) {
    if(id == null) {
      LOG.error(
          "Interchange mapping failed, the required field {} is missing. Interchange id: {}",
          fieldName,
          rootId
      );
      throw new ExitMappingException();
    }
    return idFactory.createId(id);
  }

  private static <T> void assertRefExist(String entity, String fieldName, String rootId, String id, T instance) {
    if(instance == null) {
      LOG.error(
          "Interchange mapping failed, {} entity not found. Interchange id={}, ref {}={}",
          entity,
          rootId,
          fieldName,
          id
      );
      throw new ExitMappingException();
    }
  }

  private static class ExitMappingException extends RuntimeException {}
}
