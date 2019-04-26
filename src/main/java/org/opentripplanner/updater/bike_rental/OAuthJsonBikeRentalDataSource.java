package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Additional logic for fetching and refreshing OAuth access token
 */

public abstract class OAuthJsonBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(OAuthJsonBikeRentalDataSource.class);
    private final String publicId;
    private final String secret;
    private final String accessTokenBaseUrl;
    private String accessToken;

    public OAuthJsonBikeRentalDataSource(String jsonPath, String publicId, String secret, String accessTokenUrl) {
        super(jsonPath);
        this.publicId = publicId;
        this.secret = secret;
        this.accessTokenBaseUrl = accessTokenUrl;
    }

    @Override
    public boolean update() {
        boolean updateSuccessful = false;
        if (accessToken != null) {
            setUrl(super.getUrl().concat("?access_token=").concat(accessToken));
            updateSuccessful = super.update();
        }
        if (!updateSuccessful) {
            updateAccessToken();
        }
        return updateSuccessful;
    }

    private void updateAccessToken() {
        try {
            String accessTokenUrl = accessTokenBaseUrl
                    .concat("?client_id=")
                    .concat(publicId)
                    .concat("&client_secret=")
                    .concat(secret)
                    .concat("&grant_type=client_credentials");
            InputStream dataStream = HttpUtils.getData(accessTokenUrl, null, null);
            String jsonString = convertStreamToString(dataStream);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonString);
            this.accessToken = rootNode.path("access_token").asText();
            log.info("Access token updated");
        } catch (Exception e) {
            log.warn("Could not get access token for bike rental updater");
        }
    }
}
