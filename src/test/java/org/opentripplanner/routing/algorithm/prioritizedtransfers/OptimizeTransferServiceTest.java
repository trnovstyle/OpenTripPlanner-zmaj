package org.opentripplanner.routing.algorithm.prioritizedtransfers;

import org.junit.Test;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.model.StopTime;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.model.TripStopTime;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.model.TripToTripTransfer;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.TransfersPermutationService;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.TripToTripTransfersService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.COST_CALCULATOR;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.SLACK_PROVIDER;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TRIP_1;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TRIP_2;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TRIP_3;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TX_DURATION;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

public class OptimizeTransferServiceTest implements RaptorTestConstants {
  public static final List<TripToTripTransfer<TestTripSchedule>> t2tTransfers = List.of(
      new TripToTripTransfer<>(
          TripStopTime.arrival(TRIP_1, STOP_B, 1),
          TripStopTime.departure(TRIP_2, STOP_C, 0),
          walk(STOP_D, TX_DURATION)
      ),
      new TripToTripTransfer<>(
          TripStopTime.arrival(TRIP_2, STOP_D, 1),
          TripStopTime.departure(TRIP_3, STOP_D, 0),
          null
      )
  );

  private final Map<Integer, List<TripToTripTransfer<TestTripSchedule>>> transfers = t2tTransfers
      .stream()
      .collect(Collectors.groupingBy(it -> it.from().stop()));



  private final TripToTripTransfersService<TestTripSchedule> transfersService =
      new TripToTripTransfersService<>(null, null) {
    @Override
    public List<TripToTripTransfer<TestTripSchedule>> findTransfers(
        TestTripSchedule ft, StopTime from, TestTripSchedule tt
    ) {
      return transfers.getOrDefault(from.stop(), List.of());
    }
  };

  private final TransfersPermutationService<TestTripSchedule> transfersPermutationService =
      new TransfersPermutationService<>(transfersService, COST_CALCULATOR, SLACK_PROVIDER);

  private final OptimizeTransferService<TestTripSchedule> subject = new OptimizeTransferService<>(

      null,
      transfersPermutationService,
      null,
      false
  );

  @Test
  public void optimizeTransfersMatchesTheOriginalPathWithExactSameCost() {
    // Given
    Path<TestTripSchedule> original = BasicPathTestCase.basicTripAsPath();

    // When
    var result = subject.optimize(List.of(original));

    assertEquals(result.toString(), 1, result.size());

    // Compare each leg in reverse from egress to access to detect first difference.
    // We start with the tail, because the path is build that way. An error in the egress could
    // propagate forward all the way to the access
    var path = result.get(0);
    var oldLegs = original.legStream().collect(Collectors.toList());
    var newLegs = path.legStream().collect(Collectors.toList());

    for (int i = oldLegs.size()-1; i>=0; i--) {
      var oldLeg = oldLegs.get(i);
      var newLeg = newLegs.get(i);

      assertEquals(oldLeg.toString(), newLeg.toString());
      assertEquals(
          "Cost: " + oldLeg + " != " + newLeg ,
          oldLeg.generalizedCost(),
          newLeg.generalizedCost()
      );
    }

    // Total cost is part of path toString()
    assertEquals(original.toString(), path.toString());
  }
}