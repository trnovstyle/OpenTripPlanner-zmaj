package org.opentripplanner.index.transmodel.mapping;

import org.opentripplanner.index.transmodel.model.TransmodelTransportSubmode;

import java.util.HashMap;
import java.util.Map;

public class TransportSubmodeMapper {

    private Map<TransmodelTransportSubmode, Integer> fromTransmodelMap = new HashMap<>();

    private Map<Integer, TransmodelTransportSubmode> fromOtpMap = new HashMap<>();

    private static final TransmodelTransportSubmode DEFAULT_TRANSMODEL_VALUE = TransmodelTransportSubmode.UNKNOWN;

    // TODO do we need this?
    // 3= BUS
    private static final Integer DEFAULT_OTP_VALUE = 3;

    public TransportSubmodeMapper() {
        init();
    }

    public TransmodelTransportSubmode toTransmodel(int otpMode) {
        TransmodelTransportSubmode transmodelTransportSubmode = fromOtpMap.get(otpMode);
        if (transmodelTransportSubmode != null) {
            return transmodelTransportSubmode;
        }
        return DEFAULT_TRANSMODEL_VALUE;
    }

    public int fromTransmodel(TransmodelTransportSubmode transmodelTransportSubmode) {
        Integer otpTransportSubmode = fromTransmodelMap.get(transmodelTransportSubmode);
        if (otpTransportSubmode != null) {
            return otpTransportSubmode;
        }
        return DEFAULT_OTP_VALUE;
    }


    private void init() {

        addTwoWayMapping(TransmodelTransportSubmode.LOCAL, 100);
        addTwoWayMapping(TransmodelTransportSubmode.HIGH_SPEED_RAIL, 101);
        addTwoWayMapping(TransmodelTransportSubmode.LONG_DISTANCE, 102);
        addTwoWayMapping(TransmodelTransportSubmode.CAR_TRANSPORT_RAIL_SERVICE, 104);
        addTwoWayMapping(TransmodelTransportSubmode.SLEEPER_RAIL_SERVICE, 105);
        addTwoWayMapping(TransmodelTransportSubmode.REGIONAL_RAIL, 106);
        addTwoWayMapping(TransmodelTransportSubmode.TOURIST_RAILWAY, 107);
        addTwoWayMapping(TransmodelTransportSubmode.RAIL_SHUTTLE, 108);
        addTwoWayMapping(TransmodelTransportSubmode.SUBURBAN_RAILWAY, 109);
        addTwoWayMapping(TransmodelTransportSubmode.REPLACEMENT_RAIL_SERVICE, 110);
        addTwoWayMapping(TransmodelTransportSubmode.SPECIAL_TRAIN, 111);
        addTwoWayMapping(TransmodelTransportSubmode.CROSS_COUNTRY_RAIL, 114);
        addTwoWayMapping(TransmodelTransportSubmode.RACK_AND_PINION_RAILWAY, 116);

        addTwoWayMapping(TransmodelTransportSubmode.INTERNATIONAL_COACH, 201);
        addTwoWayMapping(TransmodelTransportSubmode.NATIONAL_COACH, 202);
        addTwoWayMapping(TransmodelTransportSubmode.SHUTTLE_COACH, 203);
        addTwoWayMapping(TransmodelTransportSubmode.REGIONAL_COACH, 204);
        addTwoWayMapping(TransmodelTransportSubmode.SPECIAL_COACH, 205);
        addTwoWayMapping(TransmodelTransportSubmode.SIGHTSEEING_COACH, 206);
        addTwoWayMapping(TransmodelTransportSubmode.TOURIST_COACH, 207);
        addTwoWayMapping(TransmodelTransportSubmode.COMMUTER_COACH, 208);

        addTwoWayMapping(TransmodelTransportSubmode.URBAN_RAILWAY, 400);
        addTwoWayMapping(TransmodelTransportSubmode.METRO, 401);
        addTwoWayMapping(TransmodelTransportSubmode.TUBE, 402);


        addTwoWayMapping(TransmodelTransportSubmode.REGIONAL_BUS, 701);
        addTwoWayMapping(TransmodelTransportSubmode.EXPRESS_BUS, 702);
        addTwoWayMapping(TransmodelTransportSubmode.LOCAL_BUS, 704);
        addTwoWayMapping(TransmodelTransportSubmode.NIGHT_BUS, 705);
        addTwoWayMapping(TransmodelTransportSubmode.POST_BUS, 706);
        addTwoWayMapping(TransmodelTransportSubmode.SPECIAL_NEEDS_BUS, 707);
        addTwoWayMapping(TransmodelTransportSubmode.MOBILITY_BUS, 708);
        addTwoWayMapping(TransmodelTransportSubmode.MOBILITY_BUS_FOR_REGISTERED_DISABLED, 709);
        addTwoWayMapping(TransmodelTransportSubmode.SIGHTSEEING_BUS, 710);
        addTwoWayMapping(TransmodelTransportSubmode.SHUTTLE_BUS, 711);
        addTwoWayMapping(TransmodelTransportSubmode.SCHOOL_BUS, 712);
        addTwoWayMapping(TransmodelTransportSubmode.SCHOOL_AND_PUBLIC_SERVICE_BUS, 713);
        addTwoWayMapping(TransmodelTransportSubmode.RAIL_REPLACEMENT_BUS, 714);
        addTwoWayMapping(TransmodelTransportSubmode.DEMAND_AND_RESPONSE_BUS, 715);

        addTwoWayMapping(TransmodelTransportSubmode.CITY_TRAM, 901);
        addTwoWayMapping(TransmodelTransportSubmode.LOCAL_TRAM, 902);
        addTwoWayMapping(TransmodelTransportSubmode.REGIONAL_TRAM, 903);
        addTwoWayMapping(TransmodelTransportSubmode.SIGHTSEEING_TRAM, 904);
        addTwoWayMapping(TransmodelTransportSubmode.SHUTTLE_TRAM, 905);

        addTwoWayMapping(TransmodelTransportSubmode.INTERNATIONAL_CAR_FERRY, 1001);
        addTwoWayMapping(TransmodelTransportSubmode.NATIONAL_CAR_FERRY, 1002);
        addTwoWayMapping(TransmodelTransportSubmode.REGIONAL_CAR_FERRY, 1003);
        addTwoWayMapping(TransmodelTransportSubmode.LOCAL_CAR_FERRY, 1004);
        addTwoWayMapping(TransmodelTransportSubmode.INTERNATIONAL_PASSENGER_FERRY, 1005);
        addTwoWayMapping(TransmodelTransportSubmode.NATIONAL_PASSENGER_FERRY, 1006);
        addTwoWayMapping(TransmodelTransportSubmode.REGIONAL_PASSENGER_FERRY, 1007);
        addTwoWayMapping(TransmodelTransportSubmode.LOCAL_PASSENGER_FERRY, 1008);
        addTwoWayMapping(TransmodelTransportSubmode.POST_BOAT, 1009);
        addTwoWayMapping(TransmodelTransportSubmode.TRAIN_FERRY, 1010);
        addTwoWayMapping(TransmodelTransportSubmode.ROAD_FERRY_LINK, 1011);
        addTwoWayMapping(TransmodelTransportSubmode.AIRPORT_BOAT_LINK, 1012);
        addTwoWayMapping(TransmodelTransportSubmode.HIGH_SPEED_VEHICLE_SERVICE, 1013);
        addTwoWayMapping(TransmodelTransportSubmode.HIGH_SPEED_PASSENGER_SERVICE, 1014);
        addTwoWayMapping(TransmodelTransportSubmode.SIGHTSEEING_SERVICE, 1015);
        addTwoWayMapping(TransmodelTransportSubmode.SCHOOL_BOAT, 1016);
        addTwoWayMapping(TransmodelTransportSubmode.CABLE_FERRY, 1017);
        addTwoWayMapping(TransmodelTransportSubmode.RIVER_BUS, 1018);
        addTwoWayMapping(TransmodelTransportSubmode.SCHEDULED_FERRY, 1019);
        addTwoWayMapping(TransmodelTransportSubmode.SHUTTLE_FERRY_SERVICE, 1020);

        addTwoWayMapping(TransmodelTransportSubmode.INTERNATIONAL_FLIGHT, 1101);
        addTwoWayMapping(TransmodelTransportSubmode.DOMESTIC_FLIGHT, 1102);
        addTwoWayMapping(TransmodelTransportSubmode.INTERCONTINENTAL_FLIGHT, 1103);
        addTwoWayMapping(TransmodelTransportSubmode.DOMESTIC_SCHEDULED_FLIGHT, 1104);
        addTwoWayMapping(TransmodelTransportSubmode.SHUTTLE_FLIGHT, 1105);
        addTwoWayMapping(TransmodelTransportSubmode.INTERCONTINENTAL_CHARTER_FLIGHT, 1106);
        addTwoWayMapping(TransmodelTransportSubmode.INTERNATIONAL_CHARTER_FLIGHT, 1107);
        addTwoWayMapping(TransmodelTransportSubmode.ROUND_TRIP_CHARTER_FLIGHT, 1108);
        addTwoWayMapping(TransmodelTransportSubmode.SIGHTSEEING_FLIGHT, 1109);
        addTwoWayMapping(TransmodelTransportSubmode.HELICOPTER_SERVICE, 1110);
        addTwoWayMapping(TransmodelTransportSubmode.DOMESTIC_CHARTER_FLIGHT, 1111);
        addTwoWayMapping(TransmodelTransportSubmode.SCHENGEN_AREA_FLIGHT, 1112);
        addTwoWayMapping(TransmodelTransportSubmode.AIRSHIP_SERVICE, 1113);


        addTwoWayMapping(TransmodelTransportSubmode.TELECABIN, 1301);
        addTwoWayMapping(TransmodelTransportSubmode.CABLE_CAR, 1302);
        addTwoWayMapping(TransmodelTransportSubmode.LIFT, 1303);
        addTwoWayMapping(TransmodelTransportSubmode.CHAIR_LIFT, 1304);
        addTwoWayMapping(TransmodelTransportSubmode.DRAG_LIFT, 1305);

        addTwoWayMapping(TransmodelTransportSubmode.FUNICULAR, 1401);
        addTwoWayMapping(TransmodelTransportSubmode.ALL_FUNICULAR_SERVICES, 1402);


        addTwoWayMapping(TransmodelTransportSubmode.COMMUNAL_TAXI, 1501);
        addTwoWayMapping(TransmodelTransportSubmode.WATER_TAXI, 1502);
        addTwoWayMapping(TransmodelTransportSubmode.RAIL_TAXI, 1503);
        addTwoWayMapping(TransmodelTransportSubmode.BIKE_TAXI, 1504);

        addTwoWayMapping(TransmodelTransportSubmode.HIRE_CAR, 1601);
        addTwoWayMapping(TransmodelTransportSubmode.HIRE_VAN, 1602);
        addTwoWayMapping(TransmodelTransportSubmode.HIRE_MOTORBIKE, 1603);
        addTwoWayMapping(TransmodelTransportSubmode.HIRE_CYCLE, 1604);
    }

    private void addTwoWayMapping(TransmodelTransportSubmode transmodelValue, Integer otpValue) {
        fromTransmodelMap.put(transmodelValue, otpValue);
        fromOtpMap.put(otpValue, transmodelValue);
    }


}
