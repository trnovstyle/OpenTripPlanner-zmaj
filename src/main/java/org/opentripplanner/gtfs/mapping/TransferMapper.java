package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfers.Transfer;
import org.opentripplanner.model.transfers.TransferPriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Responsible for mapping GTFS Transfer into the OTP model. */
class TransferMapper {
    /**
     * This transfer is recommended over other transfers. The routing algorithm should prefer
     * this transfer compared to other transfers, for example by assigning a lower weight to it.
     */
    private static final int RECOMMENDED = 0;

    /**
     * This means the departing vehicle will wait for the arriving one and leave sufficient time
     * for a rider to transfer between routes.
     */
    private static final int GUARANTEED = 1;

    /**
     * This is a regular transfer that is defined in the transit data (as opposed to
     * OpenStreetMap data). In the case that both are present, this should take precedence.
     * Because the the duration of the transfer is given and not the distance, walk speed will
     * have no effect on this.
     */
    private static final int MIN_TIME = 2;

    /**
     * Transfers between these stops (and route/trip) is not possible (or not allowed), even if
     * a transfer is already defined via OpenStreetMap data or in transit data.
     */
    private static final int FORBIDDEN = 3;


    private final RouteMapper routeMapper;

    private final StationMapper stationMapper;

    private final StopMapper stopMapper;

    private final TripMapper tripMapper;

    TransferMapper(
            RouteMapper routeMapper,
            StationMapper stationMapper,
            StopMapper stopMapper,
            TripMapper tripMapper
    ) {
        this.routeMapper = routeMapper;
        this.stationMapper = stationMapper;
        this.stopMapper = stopMapper;
        this.tripMapper = tripMapper;
    }

    Collection<Transfer> map(Collection<org.onebusaway.gtfs.model.Transfer> allTransfers) {
        return allTransfers.stream().flatMap(t -> this.map(t).stream()).collect(Collectors.toList());
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    Collection<Transfer> map(org.onebusaway.gtfs.model.Transfer orginal) {
        return orginal == null ? List.of() : doMap(orginal);
    }

    private Collection<Transfer> doMap(org.onebusaway.gtfs.model.Transfer rhs) {

        Trip fromTrip = tripMapper.map(rhs.getFromTrip());
        Trip toTrip = tripMapper.map(rhs.getToTrip());
        Route fromRoute = routeMapper.map(rhs.getFromRoute());
        Route toRoute = routeMapper.map(rhs.getToRoute());

        boolean guaranteed = rhs.getTransferType() == GUARANTEED;
        boolean staySeated = sameBlockId(fromTrip, toTrip);

        TransferPriority transferPriority = mapTypeToPriority(rhs.getTransferType());
        int transferTime = rhs.getMinTransferTime();

        // Transfers may be specified using parent stations
        // (https://developers.google.com/transit/gtfs/reference/transfers-file)
        // "If the stop ID refers to a station that contains multiple stops, this transfer rule
        // applies to all stops in that station." we thus expand transfers that use parent stations
        // to all the member stops.

        Collection<Stop> fromStops = getStopOrChildStops(rhs.getFromStop());
        Collection<Stop> toStops = getStopOrChildStops(rhs.getToStop());

        Collection<Transfer> lhs = new ArrayList<>();

        for (Stop fromStop : fromStops) {
            for (Stop toStop : toStops ) {
                lhs.add(
                        new Transfer(
                                fromStop,
                                toStop,
                                fromRoute,
                                toRoute,
                                fromTrip,
                                toTrip,
                                staySeated,
                                guaranteed,
                                transferPriority,
                                transferTime
                        )
                );
            }
        }
        return lhs;
    }

    private Collection<Stop> getStopOrChildStops(org.onebusaway.gtfs.model.Stop gtfsStop) {
        if (gtfsStop.getLocationType() == 0) {
            return Collections.singletonList(stopMapper.map(gtfsStop));
        } else {
            return stationMapper.map(gtfsStop).getChildStops();
        }
    }

    static TransferPriority mapTypeToPriority(int type) {
        switch (type) {
            case FORBIDDEN:
                return TransferPriority.NOT_ALLOWED;
            case GUARANTEED:
            case MIN_TIME:
                return TransferPriority.ALLOWED;
            case RECOMMENDED:
                return TransferPriority.RECOMMENDED;
        }
        throw new IllegalArgumentException("Mapping missing for type: "  + type);
    }

    private boolean sameBlockId(Trip a, Trip b) {
        if(a == null || b == null) { return false; }
        return a.getBlockId() != null && a.getBlockId().equals(b.getBlockId());
    }
}
