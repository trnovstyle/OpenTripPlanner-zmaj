package org.opentripplanner.index.transmodel.mapping;

import org.opentripplanner.index.transmodel.model.TransmodelStopPlaceType;

public class StopPlaceTypeMapper {

// TODO no support
    public static TransmodelStopPlaceType getStopPlaceType(int gtfsVehicleType) {
        /* TPEG Extension  https://groups.google.com/d/msg/gtfs-changes/keT5rTPS7Y0/71uMz2l6ke0J */
        if (gtfsVehicleType >= 100 && gtfsVehicleType < 200) { // Railway Service
            return TransmodelStopPlaceType.RAIL_STATION;
        } else if (gtfsVehicleType >= 200 && gtfsVehicleType < 300) { //Coach Service
            return TransmodelStopPlaceType.ONSTREET_BUS;
        } else if (gtfsVehicleType >= 300 && gtfsVehicleType < 500) { //Suburban Railway Service and Urban Railway service
            if (gtfsVehicleType >= 401 && gtfsVehicleType <= 402) {
                return TransmodelStopPlaceType.METRO_STATION;
            }
            return TransmodelStopPlaceType.RAIL_STATION;
        } else if (gtfsVehicleType >= 500 && gtfsVehicleType < 700) { //Metro Service and Underground Service
            return TransmodelStopPlaceType.METRO_STATION;
        } else if (gtfsVehicleType >= 700 && gtfsVehicleType < 900) { //Bus Service and Trolleybus service
            return TransmodelStopPlaceType.ONSTREET_BUS;
        } else if (gtfsVehicleType >= 900 && gtfsVehicleType < 1000) { //Tram service
            return TransmodelStopPlaceType.ONSTREET_TRAM;
        } else if (gtfsVehicleType >= 1000 && gtfsVehicleType < 1100) { //Water Transport Service
            return TransmodelStopPlaceType.FERRY_STOP;
        } else if (gtfsVehicleType >= 1100 && gtfsVehicleType < 1200) { //Air Service
            return TransmodelStopPlaceType.AIRPORT;
        } else if (gtfsVehicleType >= 1200 && gtfsVehicleType < 1300) { //Ferry Service
            return TransmodelStopPlaceType.FERRY_STOP;
        } else if (gtfsVehicleType >= 1300 && gtfsVehicleType < 1400) { //Telecabin Service
            return TransmodelStopPlaceType.OTHER;
        } else if (gtfsVehicleType >= 1400 && gtfsVehicleType < 1500) { //Funicalar Service
            return TransmodelStopPlaceType.OTHER;
        } else if (gtfsVehicleType >= 1500 && gtfsVehicleType < 1600) { //Taxi Service
            throw new IllegalArgumentException("Taxi service not supported" + gtfsVehicleType);
        } else if (gtfsVehicleType >= 1600 && gtfsVehicleType < 1700) { //Self drive
            return TransmodelStopPlaceType.OTHER;
        }
        /* Original GTFS route types. Should these be checked before TPEG types? */
        switch (gtfsVehicleType) {
            case 0:
                return TransmodelStopPlaceType.ONSTREET_TRAM;
            case 1:
                return TransmodelStopPlaceType.METRO_STATION;
            case 2:
                return TransmodelStopPlaceType.RAIL_STATION;
            case 3:
                return TransmodelStopPlaceType.ONSTREET_BUS;
            case 4:
                return TransmodelStopPlaceType.FERRY_STOP;
            case 5:
                return TransmodelStopPlaceType.LIFT_STATION;
            case 6:
                return TransmodelStopPlaceType.OTHER;
            case 7:
                return TransmodelStopPlaceType.OTHER;
            default:
                throw new IllegalArgumentException("unknown gtfs route type " + gtfsVehicleType);
        }
    }
}
