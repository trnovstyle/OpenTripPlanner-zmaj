package org.opentripplanner.netex.mapping;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TransitSubMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class TransportModeMapper {

    public T2<TransitMode, TransitSubMode> map(
            AllVehicleModesOfTransportEnumeration netexMode,
            TransportSubmodeStructure submode
    ) {
        if (submode != null) {
            return mapModeAndSubMode(submode);
        }
        return new T2<>(mapAllVehicleModesOfTransport(netexMode), TransitSubMode.UNKNOWN);
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
                throw new IllegalArgumentException(mode.toString());
        }
    }

    private T2<TransitMode, TransitSubMode> mapModeAndSubMode(TransportSubmodeStructure submodeStructure) {
        if (submodeStructure.getAirSubmode() != null) {
            return new T2<>(TransitMode.AIRPLANE, TransitSubMode.safeValueOf(submodeStructure.getAirSubmode().value()));
        } else if (submodeStructure.getBusSubmode() != null) {
            return new T2<>(TransitMode.BUS, TransitSubMode.safeValueOf(submodeStructure.getBusSubmode().value()));
        } else if (submodeStructure.getTelecabinSubmode() != null) {
            return new T2<>(TransitMode.GONDOLA, TransitSubMode.safeValueOf(submodeStructure.getTelecabinSubmode().value()));
        } else if (submodeStructure.getCoachSubmode() != null) {
            return new T2<>(TransitMode.COACH, TransitSubMode.safeValueOf(submodeStructure.getCoachSubmode().value()));
        } else if (submodeStructure.getFunicularSubmode() != null) {
            return new T2<>(TransitMode.FUNICULAR, TransitSubMode.safeValueOf(submodeStructure.getFunicularSubmode().value()));
        } else if (submodeStructure.getMetroSubmode() != null) {
            return new T2<>(TransitMode.SUBWAY, TransitSubMode.safeValueOf(submodeStructure.getMetroSubmode().value()));
        } else if (submodeStructure.getRailSubmode() != null) {
            return new T2<>(TransitMode.RAIL, TransitSubMode.safeValueOf(submodeStructure.getRailSubmode().value()));
        } else if (submodeStructure.getTramSubmode() != null) {
            return new T2<>(TransitMode.TRAM, TransitSubMode.safeValueOf(submodeStructure.getTramSubmode().value()));
        } else if (submodeStructure.getWaterSubmode() != null) {
            return new T2<>(TransitMode.FERRY, TransitSubMode.safeValueOf(submodeStructure.getWaterSubmode().value()));
        }

        throw new IllegalArgumentException();
    }
}
