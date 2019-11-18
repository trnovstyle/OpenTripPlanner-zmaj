/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.alerts;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.SiriFuzzyTripMatcher;
import org.opentripplanner.updater.SiriHelper;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.Siri;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SiriSXUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);

    private GraphUpdaterManager updaterManager;

    private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);

    private String url;

    private String feedId;

    private SiriFuzzyTripMatcher fuzzyTripMatcher;

    private AlertPatchService alertPatchService;

    private long earlyStart;

    private AlertsUpdateHandler updateHandler = null;

    private String requestorRef;

    private int timeout;

    private static Map<String, String> requestHeaders;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        // TODO: add options to choose different patch services
        AlertPatchService alertPatchService = new AlertPatchServiceImpl(graph);
        this.alertPatchService = alertPatchService;
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }

        this.requestorRef = config.path("requestorRef").asText();
        if (requestorRef == null || requestorRef.isEmpty()) {
            requestorRef = "otp-"+UUID.randomUUID().toString();
        }

        this.url = url;// + uniquenessParameter;
        this.earlyStart = config.path("earlyStartSec").asInt(0);
        this.feedId = config.path("feedId").asText();


        int timeoutSec = config.path("timeoutSec").asInt();
        if (timeoutSec > 0) {
            this.timeout = 1000*timeoutSec;
        }

        this.fuzzyTripMatcher = new SiriFuzzyTripMatcher(graph.index);


        // TODO: Make custom headers configurable
        requestHeaders = new HashMap<>();
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            hostname = "otp-"+UUID.randomUUID().toString();
        }

        requestHeaders.put("ET-Client-Name", hostname + "-SX");

        LOG.info("Creating real-time alert updater (SIRI SX) running every {} seconds : {}", frequencySec, url);
    }

    @Override
    public void setup() {
        if (updateHandler == null) {
            updateHandler = new AlertsUpdateHandler();
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setFeedId(feedId);
        updateHandler.setAlertPatchService(alertPatchService);
        updateHandler.setSiriFuzzyTripMatcher(fuzzyTripMatcher);
    }

    @Override
    protected void runPolling() throws Exception {
        Siri updates = getUpdates();

        if (updates != null && updates.getServiceDelivery().getSituationExchangeDeliveries() != null) {
            // Handle trip updates via graph writer runnable
            // Handle update in graph writer runnable
            if (blockReadinessUntilInitialized && !isInitialized) {
                LOG.info("Execute blocking tripupdates");
                updaterManager.executeBlocking(graph -> updateHandler.update(updates.getServiceDelivery()));
            } else {
                updaterManager.execute(graph -> updateHandler.update(updates.getServiceDelivery()));
            }
        }
        if (updates != null &&
                updates.getServiceDelivery() != null &&
                updates.getServiceDelivery().isMoreData() != null &&
                updates.getServiceDelivery().isMoreData()) {
            LOG.info("More data is available - fetching immediately");
            runPolling();
        }
    }

    private Siri getUpdates() {

        long t1 = System.currentTimeMillis();
        long creating = 0;
        long fetching = 0;
        long unmarshalling = 0;
        try {
            String sxServiceRequest = SiriHelper.createSXServiceRequestAsXml(requestorRef);
            creating = System.currentTimeMillis()-t1;
            t1 = System.currentTimeMillis();

            InputStream is = HttpUtils.postData(url, sxServiceRequest, timeout, requestHeaders);

            fetching = System.currentTimeMillis()-t1;
            t1 = System.currentTimeMillis();

            Siri siri = SiriHelper.unmarshal(is);

            unmarshalling = System.currentTimeMillis()-t1;
            if (siri == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }
            ServiceDelivery serviceDelivery = siri.getServiceDelivery();
            if (serviceDelivery == null) {
                throw new RuntimeException("Failed to get serviceDelivery " + url);
            }

            ZonedDateTime responseTimestamp = serviceDelivery.getResponseTimestamp();
            if (responseTimestamp.isBefore(lastTimestamp)) {
                LOG.info("Ignoring feed with an old timestamp.");
                return null;
            }

            lastTimestamp = responseTimestamp;
            return siri;
        } catch (Exception e) {
            LOG.info("Failed after {} ms", (System.currentTimeMillis()-t1));
            LOG.error("Error reading SIRI feed from " + url, e);
        } finally {
            LOG.info("Updating SX [{}]: Create req: {}, Fetching data: {}, Unmarshalling: {}", requestorRef, creating, fetching, unmarshalling);
        }
        return null;
    }

    @Override
    public void teardown() {
    }

    public AlertPatchService getAlertPatchService() {
        return alertPatchService;
    }

    public String toString() {
        return "SiriSXUpdater (" + url + ")";
    }
}
