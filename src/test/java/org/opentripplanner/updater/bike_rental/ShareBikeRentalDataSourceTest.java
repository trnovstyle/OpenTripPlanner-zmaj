package org.opentripplanner.updater.bike_rental;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import static org.junit.Assert.*;

public class ShareBikeRentalDataSourceTest {

    @Test
    public void setUrlShouldNotEraseQueryParams() throws UnsupportedEncodingException, MalformedURLException {

        ShareBikeRentalDataSource shareBikeRentalDataSource = new ShareBikeRentalDataSource();
        shareBikeRentalDataSource.setUrl("http://map.webservice.sharebike.com:8888/json/MapService/LiveStationData?APIKey=123456&SystemID=citytrondheim");

        assertTrue("APIKey should not be removed from the URL", shareBikeRentalDataSource.getUrl().contains("APIKey"));
        assertTrue("SystemId should not be removed from the URL", shareBikeRentalDataSource.getUrl().contains("SystemID"));
        assertTrue("citybiketrondheim should not be removed from the URL", shareBikeRentalDataSource.getUrl().contains("citytrondheim"));
    }
}