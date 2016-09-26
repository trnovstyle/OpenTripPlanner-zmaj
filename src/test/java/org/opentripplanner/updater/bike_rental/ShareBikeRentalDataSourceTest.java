package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShareBikeRentalDataSourceTest {

    @Test
    public void setUrlShouldNotEraseQueryParams() throws UnsupportedEncodingException, MalformedURLException {

        ShareBikeRentalDataSource shareBikeRentalDataSource = new ShareBikeRentalDataSource();
        shareBikeRentalDataSource.setUrl("http://map.webservice.sharebike.com:8888/json/MapService/LiveStationData?APIKey=123456&SystemID=citytrondheim");

        assertTrue("APIKey should not be removed from the URL", shareBikeRentalDataSource.getUrl().contains("APIKey"));
        assertTrue("SystemId should not be removed from the URL", shareBikeRentalDataSource.getUrl().contains("SystemID"));
        assertTrue("citybiketrondheim should not be removed from the URL", shareBikeRentalDataSource.getUrl().contains("citytrondheim"));
    }

    @Test
    public void bikeRentalStationShouldHaveId() throws UnsupportedEncodingException, MalformedURLException {
        String systemId = "citybiketrondheim";

        ShareBikeRentalDataSource shareBikeRentalDataSource = new ShareBikeRentalDataSource();
        shareBikeRentalDataSource.setUrl(createUrl("", systemId));

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("StationID", 321);
        objectNode.put("Online", true);
        BikeRentalStation bikeRentalStation = shareBikeRentalDataSource.makeStation(objectNode);

        assertEquals(systemId+"_321", bikeRentalStation.id);
    }

    @Test
    public void bikeRentalStationShouldHaveAvailableCount() throws UnsupportedEncodingException, MalformedURLException {
        ShareBikeRentalDataSource shareBikeRentalDataSource = new ShareBikeRentalDataSource();
        shareBikeRentalDataSource.setUrl(createUrl("", ""));

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("Online", true);
        objectNode.put("AvailableBikeCount", 3);
        objectNode.put("AvailableSlotCount", 8);
        BikeRentalStation bikeRentalStation = shareBikeRentalDataSource.makeStation(objectNode);

        assertEquals(3, bikeRentalStation.bikesAvailable);
        assertEquals(8, bikeRentalStation.spacesAvailable);
    }

    @Test
    public void bikeRentalStationShouldHaveCoordinates() throws UnsupportedEncodingException, MalformedURLException {
        ShareBikeRentalDataSource shareBikeRentalDataSource = new ShareBikeRentalDataSource();
        shareBikeRentalDataSource.setUrl(createUrl("", ""));

        double longitude = 10.71004;
        double latitude = 59.92758;

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("Online", true);
        objectNode.put("Longitude", longitude);
        objectNode.put("Latitude", latitude);
        BikeRentalStation bikeRentalStation = shareBikeRentalDataSource.makeStation(objectNode);

        assertEquals(longitude, bikeRentalStation.x, 0.0001);
        assertEquals(latitude, bikeRentalStation.y, 0.0001);
    }

    @Test
    public void bikeRentalStationShouldHaveName() throws UnsupportedEncodingException, MalformedURLException {
        ShareBikeRentalDataSource shareBikeRentalDataSource = new ShareBikeRentalDataSource();
        shareBikeRentalDataSource.setUrl(createUrl("", ""));

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("Online", true);
        objectNode.put("StationName", "  station name ");
        BikeRentalStation bikeRentalStation = shareBikeRentalDataSource.makeStation(objectNode);

        assertEquals("station name", bikeRentalStation.name.toString());

    }

    private String createUrl(String apiKey, String systemId) {
        return "http://map.webservice.sharebike.com:8888/json/MapService/LiveStationData?APIKey=" + apiKey + "&SystemID="+systemId;
    }
}