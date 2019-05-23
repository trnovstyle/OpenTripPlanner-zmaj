package org.opentripplanner.updater.car_park;

import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Load car park updates from the Siemens Mobility POC.
 *
 */
public class SiemensCarParkDataSource extends GenericJsonCarParkDataSource{

    private static final Logger log = LoggerFactory.getLogger(SiemensCarParkDataSource.class);

    private String url;// = "https://us-central1-carbon-1287.cloudfunctions.net/parking";

    public SiemensCarParkDataSource() {
        super("data/carParks");
    }


    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        super.configure(graph, jsonNode);
        String updateUrl = jsonNode.path("url").asText();
        if (updateUrl != null && updateUrl.length() != 0) {
            this.url = updateUrl;
        }
    }


    public CarPark makeCarPark(JsonNode node) {

        CarPark station = new CarPark();
        station.id = node.path("nsr_id").asText();
        try {
            station.name = new NonLocalizedString(node.path("name").asText());
            station.y = node.path("lat").asDouble();
            station.x = node.path("lon").asDouble();
            station.realTimeData = true;
            station.maxCapacity = node.path("capacity").asInt();
            station.spacesAvailable = node.path("num_bays_available").asInt();

            station.maxCapacityRecharging = node.path("capacity_recharging").asInt();
            station.spacesAvailableRecharging = node.path("num_recharging_bays_available").asInt();

            station.maxCapacityHandicap = node.path("capacity_handicap").asInt();
            station.spacesAvailableHandicap = node.path("num_handicap_bays_available").asInt();

            return station;
        } catch (Exception e) {
            log.warn("Error parsing car park " + station.id, e);
            return null;
        }
    }
}

