package org.opentripplanner.netex.mapping;

import com.google.common.base.Optional;
import org.rutebanken.netex.model.Parking;

public class ParkingMapper {
    public org.opentripplanner.model.Parking mapParking(Parking parking){
        org.opentripplanner.model.Parking otpParking = new org.opentripplanner.model.Parking();

        otpParking.setId(parking.getId());
        otpParking.setName(parking.getName() != null ? parking.getName().getValue() : null);
        otpParking.setDesc(parking.getDescription() != null ? parking.getDescription().getValue() : null);
        otpParking.setPrincipalCapacity(parking.getPrincipalCapacity() != null ? parking.getPrincipalCapacity().intValue() : -1);
        otpParking.setTotalCapacity(parking.getTotalCapacity() != null ? parking.getTotalCapacity().intValue() : -1);
        otpParking.setOvernightParkingPermitted(
                Optional.fromNullable(parking.isOvernightParkingPermitted()).or(false));
        otpParking.setRechargingAvailable(
                Optional.fromNullable(parking.isRechargingAvailable()).or(false));
        otpParking.setSecure(
                Optional.fromNullable(parking.isSecure()).or(false));
        otpParking.setRealTimeOccupancyAvailable(
                Optional.fromNullable(parking.isRealTimeOccupancyAvailable()).or(false));
        otpParking.setUrl(parking.getBookingUrl());
        otpParking.setFreeParkingOutOfHours(
                Optional.fromNullable(parking.isFreeParkingOutOfHours()).or(false));

        if (parking.getCentroid() != null && parking.getCentroid().getLocation() != null) {
            otpParking.setLat(parking.getCentroid().getLocation().getLatitude().doubleValue());
            otpParking.setLon(parking.getCentroid().getLocation().getLongitude().doubleValue());
        }

        if (parking.getParkingType() != null) {
            switch (parking.getParkingType()) {
                case PARK_AND_RIDE:
                    otpParking.setParkingType(org.opentripplanner.model.Parking.ParkingType.PARK_AND_RIDE);
                    break;
                default:
                    otpParking.setParkingType(org.opentripplanner.model.Parking.ParkingType.UNKNOWN);
                    break;
            }
        }
        else {
            otpParking.setParkingType(org.opentripplanner.model.Parking.ParkingType.UNKNOWN);
        }

        if (parking.getParkingVehicleTypes()!= null && parking.getParkingVehicleTypes().size() > 0) {
            switch (parking.getParkingVehicleTypes().stream().findFirst().get()) {
                case CAR:
                    otpParking.setParkingVehicleType(org.opentripplanner.model.Parking.ParkingVehicleType.CAR);
                    break;
                case MOTORCYCLE:
                    otpParking.setParkingVehicleType(org.opentripplanner.model.Parking.ParkingVehicleType.MOTOR_CYCLE);
                    break;
                case PEDAL_CYCLE:
                    otpParking.setParkingVehicleType(org.opentripplanner.model.Parking.ParkingVehicleType.PEDAL_CYCLE);
                    break;
                default:
                    otpParking.setParkingVehicleType(org.opentripplanner.model.Parking.ParkingVehicleType.UNKNOWN);
            }
        }
        else {
            otpParking.setParkingVehicleType(org.opentripplanner.model.Parking.ParkingVehicleType.UNKNOWN);
        }

        if (parking.getParkingLayout() != null) {
            switch (parking.getParkingLayout()) {
                case OPEN_SPACE:
                    otpParking.setParkingLayout(org.opentripplanner.model.Parking.ParkingLayout.OPEN_SPACE);
                    break;
                case MULTISTOREY:
                    otpParking.setParkingLayout(org.opentripplanner.model.Parking.ParkingLayout.MULTI_STOREY);
                    break;
                case UNDERGROUND:
                    otpParking.setParkingLayout(org.opentripplanner.model.Parking.ParkingLayout.UNDERGROUND);
                    break;
                case ROADSIDE:
                    otpParking.setParkingLayout(org.opentripplanner.model.Parking.ParkingLayout.ROADSIDE);
                    break;
                default:
                    otpParking.setParkingLayout(org.opentripplanner.model.Parking.ParkingLayout.UNKNOWN);
            }
        } else {
            otpParking.setParkingLayout(org.opentripplanner.model.Parking.ParkingLayout.UNKNOWN);
        }

        return otpParking;
    }
}
