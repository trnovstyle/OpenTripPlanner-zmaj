package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.FlexibleStopPlace;

public class FlexibleStopPlaceTypeMapper {
    private static final Integer DEFAULT_OTP_VALUE = 3;

    public int getTransportMode(FlexibleStopPlace flexibleStopPlace) {
        if (flexibleStopPlace.getTransportMode() != null) {
            switch (flexibleStopPlace.getTransportMode()) {
                case AIR:
                    return 1100;
                case BUS:
                    return 700;
                case CABLEWAY:
                    return 1700;
                case COACH:
                    return 200;
                case FUNICULAR:
                    return 1400;
                case METRO:
                    return 401;
                case RAIL:
                    return 100;
                case TRAM:
                    return 900;
                case WATER:
                    return 1000;
                default:
                    return DEFAULT_OTP_VALUE;
            }
        } else {
            return DEFAULT_OTP_VALUE;
        }
    }
}