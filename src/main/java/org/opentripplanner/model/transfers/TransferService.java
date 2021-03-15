package org.opentripplanner.model.transfers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

import java.io.Serializable;
import java.util.Collection;

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 */
public class TransferService implements Serializable {

    /**
     * Table which contains transfers between two stops
     */
    protected Multimap<P2<Stop>, Transfer> table = ArrayListMultimap.create();

    public Transfer findTransfer(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip) {
        Collection<Transfer> transfers = table.get(new P2<>(fromStop, toStop));
        Transfer bestTransfer = null;
        int bestRank = -1;

        for (Transfer it : transfers) {
            if (it.matches(fromStop, toStop, fromTrip, toTrip)) {
                if(it.getSpecificityRanking() > bestRank) {
                    bestTransfer = it;
                    bestRank = it.getSpecificityRanking();
                }
            }
        }
        return bestTransfer;
    }

    public Collection<Transfer> getTransfers() {
        return table.values();
    }

    public void addTransfer(Transfer transfer) {
        table.put(new P2<>(transfer.getFromStop(), transfer.getToStop()), transfer);
    }
}
