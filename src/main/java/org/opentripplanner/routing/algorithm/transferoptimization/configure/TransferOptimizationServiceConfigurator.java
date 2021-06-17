package org.opentripplanner.routing.algorithm.transferoptimization.configure;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.OptimizeTransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGenerator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferOptimizedFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferServiceAdaptor;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizePathService;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.request.McCostParams;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.OTPFeature;

/**
 * Responsible for assembly of the prioritized-transfer services.
 */
public class TransferOptimizationServiceConfigurator<T extends RaptorTripSchedule> {
  private final IntFunction<Stop> stopLookup;
  private final TransferService transferService;
  private final RaptorTransitDataProvider<T> transitDataProvider;
  private final RaptorRequest<T> raptorRequest;
  private final TransferOptimizationParameters config;


  public TransferOptimizationServiceConfigurator(
      IntFunction<Stop> stopLookup,
      TransferService transferService,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      TransferOptimizationParameters config
  ) {
    this.stopLookup = stopLookup;
    this.transferService = transferService;
    this.transitDataProvider = transitDataProvider;
    this.raptorRequest = raptorRequest;
    this.config = config;
  }

  /**
   * Scope: Request
   */
  public static <T extends RaptorTripSchedule> OptimizeTransferService<T> createOptimizeTransferService(
      IntFunction<Stop> stopLookup,
      TransferService transferService,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      TransferOptimizationParameters config
  ) {
    return new TransferOptimizationServiceConfigurator<T>(
        stopLookup,
        transferService,
        transitDataProvider,
        raptorRequest,
        config
    ).createOptimizeTransferService();
  }

  private OptimizeTransferService<T> createOptimizeTransferService() {
    var pathTransferGenerator = createPathTransferGenerator(
            config.optimizeTransferPriority()
    );
    var costCalculator = createCostCalculator();
    var filter = createTransferPointFilter(
            config.optimizeTransferPriority(), config.optimizeTransferWaitTime()
    );

    if(config.optimizeTransferWaitTime()) {
      var transferWaitTimeCalculator = createOptimizeTransferWaitTimeCalculator();

      var transfersPermutationService = createTransfersPermutationService(
              pathTransferGenerator,
              filter,
              transferWaitTimeCalculator::cost,
              costCalculator
      );

      return new OptimizeTransferService<>(
              transfersPermutationService,
              createMinSafeTxTimeService(),
              transferWaitTimeCalculator
      );
    }
    else {
      var transfersPermutationService = createTransfersPermutationService(
              pathTransferGenerator,
              filter,
              PathLeg::tailGeneralizedCost,
              costCalculator
      );
      return new OptimizeTransferService<>(transfersPermutationService);
    }
  }

  private OptimizePathService<T> createTransfersPermutationService(
          TransferGenerator<T> transferGenerator,
          MinCostFilterChain<OptimizedPathTail<T>> transferPointFilter,
          ToIntFunction<PathLeg<?>> costCalcForWaitOptimization,
          CostCalculator<T> costCalculator
  ) {
    return new OptimizePathService<>(
            transferGenerator,
            costCalculator,
            raptorRequest.slackProvider(),
            costCalcForWaitOptimization,
            transferPointFilter
    );
  }

  private MinSafeTransferTimeCalculator<T> createMinSafeTxTimeService() {
    return new MinSafeTransferTimeCalculator<>(raptorRequest.slackProvider());
  }

  private TransferGenerator<T> createPathTransferGenerator(boolean transferPriority) {
    var transferServiceAdaptor = (transferService != null && transferPriority)
            ? TransferServiceAdaptor.<T>create(stopLookup, transferService)
            : TransferServiceAdaptor.<T>noop();

    return new TransferGenerator<>(
        transferServiceAdaptor,
        raptorRequest.slackProvider(),
        transitDataProvider
    );
  }

  private TransferWaitTimeCalculator createOptimizeTransferWaitTimeCalculator() {
    return new TransferWaitTimeCalculator(
            config.waitReluctanceRouting(),
            config.inverseWaitReluctance(),
            config.minSafeWaitTimeFactor()
    );
  }

  private MinCostFilterChain<OptimizedPathTail<T>> createTransferPointFilter(
          boolean transferPriority, boolean optimizeWaitTime
  ) {
    return TransferOptimizedFilterFactory.filter(transferPriority, optimizeWaitTime);
  }

  private DefaultCostCalculator<T> createCostCalculator() {
    McCostParams p = raptorRequest.multiCriteriaCostFactors();
    return new DefaultCostCalculator<>(
        p.boardCost(),
        p.transferCost(),
        p.walkReluctanceFactor(),
        p.waitReluctanceFactor(),
        transitDataProvider.stopBoarAlightCost(),
        p.transitReluctanceFactors()
    );
  }
}
