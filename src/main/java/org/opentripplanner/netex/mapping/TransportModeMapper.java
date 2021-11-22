package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class TransportModeMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransportModeMapper.class);

    private final TransitModeService transitModeService;

    public TransportModeMapper(
        TransitModeService transitModeService
    ) {
        this.transitModeService = transitModeService;
    }

    public TransitMode map(
        AllVehicleModesOfTransportEnumeration netexMode,
        TransportSubmodeStructure submode
    ) {
        TransitMode result = null;
        if (submode != null) {
            result = mapSubmodeFromConfiguration(getSubmodeAsString(submode));
        }
        // Fallback to main mode
        if (result == null) {
            result = mapAllVehicleModesOfTransport(netexMode);
        }

        return result;
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
            result = mapVehicleMode(stopPlace.getTransportMode());
        }

        return result;
    }


    private TransitMode mapAllVehicleModesOfTransport(AllVehicleModesOfTransportEnumeration mode) {
        switch (mode) {
            case AIR:
                return TransitMode.AIRPLANE;
            case BUS:
            case TAXI:
                return TransitMode.BUS;
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
                return TransitMode.FERRY;
            default:
                throw new IllegalArgumentException();
        }
    }

    private TransitMode mapVehicleMode(VehicleModeEnumeration mode) {
        if (mode == null) {
            return null;
        }
        
        switch (mode) {
            case AIR:
                return TransitMode.AIRPLANE;
            case BUS:
                return TransitMode.BUS;
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
            case TROLLEY_BUS:
                return TransitMode.TROLLEYBUS;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String getSubmodeAsString(TransportSubmodeStructure submode) {
        if (submode.getAirSubmode() != null) {
            return submode.getAirSubmode().value();
        } else if (submode.getBusSubmode() != null) {
            return submode.getBusSubmode().value();
        } else if (submode.getTelecabinSubmode() != null) {
            return submode.getTelecabinSubmode().value();
        } else if (submode.getCoachSubmode() != null) {
            return submode.getCoachSubmode().value();
        } else if (submode.getFunicularSubmode() != null) {
            return submode.getFunicularSubmode().value();
        } else if (submode.getMetroSubmode() != null) {
            return submode.getMetroSubmode().value();
        } else if (submode.getRailSubmode() != null) {
            return submode.getRailSubmode().value();
        } else if (submode.getTramSubmode() != null) {
            return submode.getTramSubmode().value();
        } else if (submode.getWaterSubmode() != null) {
            return submode.getWaterSubmode().value();
        }
        throw new IllegalArgumentException();
    }

    private String getSubmodeAsString(StopPlace stopPlace) {
        if (stopPlace.getAirSubmode() != null) {
            return stopPlace.getAirSubmode().value();
        } else if (stopPlace.getBusSubmode() != null) {
            return stopPlace.getBusSubmode().value();
        } else if (stopPlace.getTelecabinSubmode() != null) {
            return stopPlace.getTelecabinSubmode().value();
        } else if (stopPlace.getCoachSubmode() != null) {
            return stopPlace.getCoachSubmode().value();
        } else if (stopPlace.getFunicularSubmode() != null) {
            return stopPlace.getFunicularSubmode().value();
        } else if (stopPlace.getMetroSubmode() != null) {
            return stopPlace.getMetroSubmode().value();
        } else if (stopPlace.getRailSubmode() != null) {
            return stopPlace.getRailSubmode().value();
        } else if (stopPlace.getTramSubmode() != null) {
            return stopPlace.getTramSubmode().value();
        } else if (stopPlace.getWaterSubmode() != null) {
            return stopPlace.getWaterSubmode().value();
        }
        return null;
    }

    private TransitMode mapSubmodeFromConfiguration(
        String subModeString) {

        if (transitModeService == null) {
            LOG.info("No transitModeService configured.");
            return null;
        }

        TransitMode transitMode;

        try {
            transitMode = transitModeService.getTransitModeByNetexSubMode(
                String.valueOf(subModeString));
        } catch (IllegalArgumentException e) {
            LOG.info("SubMode {} not configured. Falling back to main mode.", subModeString);
            transitMode = null;
        }

        return transitMode;
    }
}
