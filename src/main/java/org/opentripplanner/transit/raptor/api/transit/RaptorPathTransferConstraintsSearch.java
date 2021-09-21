package org.opentripplanner.transit.raptor.api.transit;


import javax.annotation.Nullable;


/**
 * This interface enable is used by Raptor to create a path from the Raptor state. We do not
 * keep constraints during the search, so we need to look it up after the search.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorPathTransferConstraintsSearch<T extends RaptorTripSchedule> {

    @Nullable
    RaptorConstrainedTransfer findConstrainedTransfer(
            T fromTrip,
            int fromStopPosition,
            T toTrip,
            int toStopPosition
    );
}
