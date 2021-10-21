package org.opentripplanner.ext.sorlandsbanen;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TripScheduleWithOffset;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConcurrentCompositeWorker<T extends RaptorTripSchedule> implements Worker<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentCompositeWorker.class);

    private final Worker<T> worker1;
    private final Worker<T> worker2;

    ConcurrentCompositeWorker(
            Worker<T> worker1,
            Worker<T> worker2
    ) {
        this.worker1 = worker1;
        this.worker2 = worker2;
    }


    @Override
    public Collection<Path<T>> route() {
        Map<PathKey, Path<T>> paths;
        if (OTPFeature.ParallelRouting.isOn()) {
            paths = Collections.synchronizedMap(new HashMap<>());
            CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> addAll(paths, worker1.route())),
                    CompletableFuture.runAsync(() -> addExtraRail(paths, worker2.route()))
            ).join();
        }
        else {
            paths = new HashMap<>();
            addAll(paths, worker1.route());
            addExtraRail(paths, worker2.route());
        }
        System.out.println();
        return paths.values();
    }

    private void addExtraRail(Map<PathKey, Path<T>> map, Collection<Path<T>> paths) {
        paths.forEach(p -> {
            if(hasRail(p)) {
                var v = map.put(new PathKey(p), p);
                LOG.debug("Ex.Rail {} : {}", (v==null ? "ADD " : "SKIP"), p);
            }
            else {
                LOG.debug("Ex. NOT Rail : {}", p);
            }
        });
    }

    private void addAll(Map<PathKey, Path<T>> map, Collection<Path<T>> paths) {
        paths.forEach(p -> {
            var v = map.put(new PathKey(p), p);
            LOG.debug("Normal  {} : {}", (v==null ? "ADD " : "SKIP"), p);
        });
    }

    private static boolean hasRail(Path<?> path) {
        return path.legStream()
                .filter(PathLeg::isTransitLeg)
                .anyMatch(leg -> {
                    var trip =  (TripScheduleWithOffset)leg.asTransitLeg().trip();
                    var mode = trip.getOriginalTripPattern().getMode().getMainMode();
                    return mode == TransitMainMode.RAIL;
                });
    }
}
