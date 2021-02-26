package org.opentripplanner.routing.algorithm.prioritizedtransfers.configure;

import org.opentripplanner.routing.algorithm.prioritizedtransfers.OptimizeTransferService;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.TransfersPermutationService;
import org.opentripplanner.routing.algorithm.prioritizedtransfers.services.TripToTripTransfersService;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.transit.raptor.api.request.McCostParams;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Responsible for assembly of the prioritized-transfer services.
 */
public class PrioritizedTransfersConfig<T extends RaptorTripSchedule> {
  private final TransitLayer transitLayer;
  private final RaptorTransitDataProvider<T> transitDataProvider;
  private final RaptorRequest<T> raptorRequest;
  private final boolean optimizeWaitingTime;

  public PrioritizedTransfersConfig(
      TransitLayer transitLayer,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      boolean optimizeWaitingTime
  ) {
    this.transitLayer = transitLayer;
    this.transitDataProvider = transitDataProvider;
    this.raptorRequest = raptorRequest;
    this.optimizeWaitingTime = optimizeWaitingTime;
  }

  /**
   * Scope: Request
   */
  public static <T extends RaptorTripSchedule> OptimizeTransferService<T> createOptimizeTransferService(
      TransitLayer transitLayer,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      boolean optimizeWaitingTime
  ) {
    return new PrioritizedTransfersConfig<>(
        transitLayer,
        transitDataProvider,
        raptorRequest,
        optimizeWaitingTime
    ).createOptimizeTransferService();
  }

  private OptimizeTransferService<T> createOptimizeTransferService() {
    var trip2tripTxService = createTripToTripTransfersService();
    var costCalculator = createCostCalculator();

    var transfersPermutationService = createTransfersPermutationService(
        trip2tripTxService,
        costCalculator
    );

    return new OptimizeTransferService<T>(
        transitLayer,
        transfersPermutationService,
        createMinSafeTxTimeService(),
        optimizeWaitingTime
    );
  }

  private TransfersPermutationService<T> createTransfersPermutationService(
      TripToTripTransfersService<T> tripToTripTransfersService,
      CostCalculator<T> costCalculator
  ) {
    return new TransfersPermutationService<>(
        tripToTripTransfersService,
        costCalculator,
        raptorRequest.slackProvider()
    );
  }

  private MinSafeTransferTimeCalculator<T> createMinSafeTxTimeService() {
    return new MinSafeTransferTimeCalculator<>(raptorRequest.slackProvider());
  }

  private TripToTripTransfersService<T> createTripToTripTransfersService() {
    return new TripToTripTransfersService<>(
        raptorRequest.slackProvider(),
        transitDataProvider
    );
  }

  private DefaultCostCalculator<T> createCostCalculator() {
    McCostParams p = raptorRequest.multiCriteriaCostFactors();
    return new DefaultCostCalculator<>(
        transitDataProvider.stopBoarAlightCost(),
        p.boardCost(),
        p.transferCost(),
        p.walkReluctanceFactor(),
        p.waitReluctanceFactor()
    );
  }
}
