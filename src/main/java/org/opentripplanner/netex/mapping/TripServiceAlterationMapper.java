package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.TripServiceAlteration;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class TripServiceAlterationMapper {
     public static TripServiceAlteration mapAlteration(ServiceAlterationEnumeration netexValue) {
        if (netexValue == null) { return null; }
        switch (netexValue) {
            case PLANNED: return TripServiceAlteration.planned;
            case CANCELLATION: return TripServiceAlteration.cancellation;
            case REPLACED: return TripServiceAlteration.replaced;
            case EXTRA_JOURNEY: return TripServiceAlteration.extraJourney;
        }
        throw new IllegalArgumentException("Unmapped alternation: " + netexValue);
    }
}