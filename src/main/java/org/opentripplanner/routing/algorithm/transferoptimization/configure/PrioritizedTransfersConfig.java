package org.opentripplanner.routing.algorithm.transferoptimization.configure;

import java.util.function.IntFunction;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfers.TransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.OptimizeTransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.services.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizeTransferCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.PriorityBasedTransfersCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransfersPermutationService;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TripToTripTransfersService;
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
  private final IntFunction<Stop> stopLookup;
  private final TransferService transferService;
  private final RaptorTransitDataProvider<T> transitDataProvider;
  private final RaptorRequest<T> raptorRequest;
  private final boolean optimizeWaitingTime;

  public PrioritizedTransfersConfig(
      IntFunction<Stop> stopLookup,
      TransferService transferService,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      boolean optimizeWaitingTime
  ) {
    this.stopLookup = stopLookup;
    this.transferService = transferService;
    this.transitDataProvider = transitDataProvider;
    this.raptorRequest = raptorRequest;
    this.optimizeWaitingTime = optimizeWaitingTime;
  }

  /**
   * Scope: Request
   */
  public static <T extends RaptorTripSchedule> OptimizeTransferService<T> createOptimizeTransferService(
      IntFunction<Stop> stopLookup,
      TransferService transferService,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      boolean optimizeWaitingTime
  ) {
    return new PrioritizedTransfersConfig<>(
        stopLookup,
        transferService,
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

    var priorityCostCalculator = priorityCostCalculator();

    if(optimizeWaitingTime) {
      return new OptimizeTransferService<>(
          transfersPermutationService,
          priorityCostCalculator,
          createMinSafeTxTimeService(),
          createOptimizeTransferCostCalculator()
      );
    }
    else {
      return new OptimizeTransferService<>(
          transfersPermutationService,
          priorityCostCalculator
      );
    }
  }

  private PriorityBasedTransfersCostCalculator<T> priorityCostCalculator() {
    return new PriorityBasedTransfersCostCalculator<>(
         stopLookup, transferService
    );
  }


  private OptimizeTransferCostCalculator createOptimizeTransferCostCalculator() {
    return new OptimizeTransferCostCalculator(
        raptorRequest.multiCriteriaCostFactors().waitReluctanceFactor(),
        4.0
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
