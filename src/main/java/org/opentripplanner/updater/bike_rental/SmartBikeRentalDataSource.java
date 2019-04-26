package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmartBikeRentalDataSource extends OAuthJsonBikeRentalDataSource {
    private String baseURL = null;

    private Set<String> networks = null;

    SmartBikeRentalDataSource(String network, String publicId, String secret, String accessTokenUrl) {
        super("stations", publicId, secret, accessTokenUrl);
        this.baseURL = getUrl();
        this.networks = new HashSet<>();
        this.networks.add(network);
    }

    /**
     *
     * Bike-rental station data source for: https://www.drammenbysykler.no/
     *
     * API description: https://www.basetis.com/ (provided by email)
     *
     */
    @Override
    public BikeRentalStation makeStation(JsonNode rentalStationNode) {
        BikeRentalStation brstation = new BikeRentalStation();

        brstation.id = rentalStationNode.path("id").toString();
        brstation.name = new NonLocalizedString(rentalStationNode.path("name").asText("").trim());
        brstation.x = rentalStationNode.path("location").path("lon").asDouble();
        brstation.y = rentalStationNode.path("location").path("lat").asDouble();

        brstation.bikesAvailable = rentalStationNode.path("availability").path("bikes").asInt();
        brstation.spacesAvailable = rentalStationNode.path("availability").path("slots").asInt();

        brstation.description = new NonLocalizedString(rentalStationNode.path("address").asText("").trim());

        brstation.networks = this.networks;
        return brstation;
    }

    @Override
    public boolean update() {
        if(baseURL == null){
            baseURL = getUrl();
        }
        setUrl(baseURL + "stations.json");
        super.setJsonParsePath("stations");
        // get stations
        if(!super.update()){
            return false;
        }
        List<BikeRentalStation> stations = super.getStations();

        // update stations with availability info
        super.stations = new ArrayList<>();
        if(baseURL == null){
            baseURL = getUrl();
        }
        setUrl(baseURL + "stations/status.json");
        super.setJsonParsePath("stationsStatus");
        if(!super.update()){
            return false;
        }

        List<BikeRentalStation> stationsAvailability = super.getStations();
        super.stations = mergeStationInfo(stations, stationsAvailability);
        return true;
    }

    private List<BikeRentalStation> mergeStationInfo(List<BikeRentalStation> stations,
                                                     List<BikeRentalStation> availabilityStations){
        List<BikeRentalStation> merged = new ArrayList<>();
        for(BikeRentalStation station : stations){
            for(BikeRentalStation availability : availabilityStations){
                if(station.id.equals(availability.id)){
                    station.bikesAvailable = availability.bikesAvailable;
                    station.spacesAvailable = availability.spacesAvailable;
                    merged.add(station);
                }
            }
        }
        return merged;
    }
}
