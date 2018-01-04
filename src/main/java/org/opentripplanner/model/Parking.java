package org.opentripplanner.model;

public final class Parking extends IdentityBean<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private double lat;

    private double lon;

    private String desc;

    private String url;

    private String parentStation;

    private int principalCapacity;

    private int totalCapacity;

    private boolean overnightParkingPermitted;

    private boolean rechargingAvailable;

    private boolean secure;

    private boolean realTimeOccupancyAvailable;

    private boolean freeParkingOutOfHours;

    private ParkingType parkingType;

    private ParkingVehicleType parkingVehicleType;

    private ParkingLayout parkingLayout;

    private ParkingReservation parkingReservation;

    public Parking() {

    }

    public Parking(Parking obj) {
        this.id = obj.id;
        this.name = obj.name;
        this.desc = obj.desc;
        this.lat = obj.lat;
        this.lon = obj.lon;
        this.url = obj.url;
        this.parentStation = obj.parentStation;
        this.principalCapacity = obj.principalCapacity;
        this.totalCapacity = obj.totalCapacity;
        this.overnightParkingPermitted = obj.overnightParkingPermitted;
        this.rechargingAvailable = obj.rechargingAvailable;
        this.secure = obj.secure;
        this.realTimeOccupancyAvailable = obj.realTimeOccupancyAvailable;
        this.freeParkingOutOfHours = obj.freeParkingOutOfHours;
        this.parkingType = obj.parkingType;
        this.parkingVehicleType = obj.parkingVehicleType;
        this.parkingLayout = obj.parkingLayout;
        this.parkingReservation = obj.parkingReservation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParentStation() {
        return parentStation;
    }

    public void setParentStation(String parentStation) {
        this.parentStation = parentStation;
    }

    public int getPrincipalCapacity() {
        return principalCapacity;
    }

    public void setPrincipalCapacity(int principalCapacity) {
        this.principalCapacity = principalCapacity;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public boolean isOvernightParkingPermitted() {
        return overnightParkingPermitted;
    }

    public void setOvernightParkingPermitted(boolean overnightParkingPermitted) {
        this.overnightParkingPermitted = overnightParkingPermitted;
    }

    public boolean isRechargingAvailable() {
        return rechargingAvailable;
    }

    public void setRechargingAvailable(boolean rechargingAvailable) {
        this.rechargingAvailable = rechargingAvailable;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isRealTimeOccupancyAvailable() {
        return realTimeOccupancyAvailable;
    }

    public void setRealTimeOccupancyAvailable(boolean realTimeOccupancyAvailable) {
        this.realTimeOccupancyAvailable = realTimeOccupancyAvailable;
    }

    public boolean isFreeParkingOutOfHours() {
        return freeParkingOutOfHours;
    }

    public void setFreeParkingOutOfHours(boolean freeParkingOutOfHours) {
        this.freeParkingOutOfHours = freeParkingOutOfHours;
    }

    public ParkingType getParkingType() {
        return parkingType;
    }

    public void setParkingType(ParkingType parkingType) {
        this.parkingType = parkingType;
    }

    public ParkingVehicleType getParkingVehicleType() {
        return parkingVehicleType;
    }

    public void setParkingVehicleType(ParkingVehicleType parkingVehicleType) {
        this.parkingVehicleType = parkingVehicleType;
    }

    public ParkingLayout getParkingLayout() {
        return parkingLayout;
    }

    public void setParkingLayout(ParkingLayout parkingLayout) {
        this.parkingLayout = parkingLayout;
    }

    public ParkingReservation getParkingReservation() {
        return parkingReservation;
    }

    public void setParkingReservation(ParkingReservation parkingReservation) {
        this.parkingReservation = parkingReservation;
    }

    @Override
    public String toString() {
        return "<Parking " + this.id + ">";
    }

    public enum ParkingType {
        PARK_AND_RIDE
    }

    public enum ParkingVehicleType {
        CAR, MOTOR_CYCLE, PEDAL_CYCLE
    }

    public enum ParkingLayout {
        OPEN_SPACE, MULTI_STOREY, UNDERGROUND, ROADSIDE
    }

    public enum ParkingReservation {
        NO_RESERVATIONS, REGISTRATION_REQUIRED, RESERVATION_REQUIRED, RESERVATION_ALLOWED
    }
}
