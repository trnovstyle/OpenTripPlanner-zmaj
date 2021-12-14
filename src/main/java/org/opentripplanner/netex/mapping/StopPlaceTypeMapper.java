package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class StopPlaceTypeMapper {

    private static final Logger LOG = LoggerFactory.getLogger(StopPlaceTypeMapper.class);

    private final TransitModeService transitModeService;

    StopPlaceTypeMapper(TransitModeService transitModeService) {
        this.transitModeService = transitModeService;
    }

    public TransitMode map(
            StopPlace stopPlace
    ) {
        TransitMode result = null;
        String submode = getSubmodeAsString(stopPlace);
        if (submode != null) {
            result = mapSubmodeFromConfiguration(submode);
        }
        // Fallback to main mode
        if (result == null) {
            result = mapVehicleMode(stopPlace);
        }

        return result;
    }

    private TransitMode mapVehicleMode(StopPlace stopPlace) {
        if (stopPlace.getTransportMode() == null) {
            return null;
        }
        switch (stopPlace.getTransportMode()) {
            case AIR:
                return TransitMode.AIRPLANE;
            case BUS:
                return TransitMode.BUS;
            case TROLLEY_BUS:
                return TransitMode.TROLLEYBUS;
            case CABLEWAY:
                return TransitMode.CABLE_CAR;
            case COACH:
                return TransitMode.COACH;
            case FUNICULAR:
                return TransitMode.FUNICULAR;
            case METRO:
                return TransitMode.SUBWAY;
            case RAIL:
                return TransitMode.RAIL;
            case TRAM:
                return TransitMode.TRAM;
            case WATER:
            case FERRY:
                return TransitMode.FERRY;
            default:
                return null;
        }
    }

    private String getSubmodeAsString(StopPlace stopPlace) {
        if (stopPlace.getAirSubmode() != null) {
            return stopPlace.getAirSubmode().value();
        }
        if (stopPlace.getBusSubmode() != null) {
            return stopPlace.getBusSubmode().value();
        }
        if (stopPlace.getTelecabinSubmode() != null) {
            return stopPlace.getTelecabinSubmode().value();
        }
        if (stopPlace.getCoachSubmode() != null) {
            return stopPlace.getCoachSubmode().value();
        }
        if (stopPlace.getFunicularSubmode() != null) {
            return stopPlace.getFunicularSubmode().value();
        }
        if (stopPlace.getMetroSubmode() != null) {
            return stopPlace.getMetroSubmode().value();
        }
        if (stopPlace.getRailSubmode() != null) {
            return stopPlace.getRailSubmode().value();
        }
        if (stopPlace.getTramSubmode() != null) {
            return stopPlace.getTramSubmode().value();
        }
        if (stopPlace.getWaterSubmode() != null) {
            return stopPlace.getWaterSubmode().value();
        }
        return null;
    }


    private TransitMode mapSubmodeFromConfiguration(String subModeString) {

        if (transitModeService == null) {
            LOG.info("No transitModeService configured.");
            return null;
        }

        TransitMode transitMode;

        try {
            transitMode = transitModeService.getTransitModeByNetexSubMode(subModeString);
        } catch (IllegalArgumentException e) {
            LOG.info("SubMode {} not configured. Falling back to main mode.", subModeString);
            transitMode = null;
        }

        return transitMode;
    }
}
