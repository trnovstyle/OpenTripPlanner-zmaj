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

package org.opentripplanner.updater.siri;

import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.entur.protobuf.mapper.SiriMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.ReadinessBlockingUpdater;
import org.opentripplanner.updater.SiriHelper;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.Siri;
import uk.org.siri.www.siri.SiriType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class starts an HTTP client which opens a websocket connection to a SIRI data source. A
 * callback is registered which handles incoming SIRI ET messages as they stream in by placing a
 * SIRI ET decoder Runnable task in the single-threaded executor for handling.
 *
 * Usage example ('websocket' name is an example) in the file 'Graph.properties':
 *
 * <pre>
 * websocket.type = websocket-siri-et-updater
 * websocket.defaultAgencyId = agency
 * websocket.url = ws://localhost:8088/siri-et
 * </pre>
 *
 */
public class WebsocketEstimatedTimetableUpdater extends ReadinessBlockingUpdater implements GraphUpdater {

    /**
     * Number of seconds to wait before checking again whether we are still connected
     */
    private static final int CHECK_CONNECTION_PERIOD_SEC = 1;

    private static final int DEFAULT_RECONNECT_PERIOD_SEC = 300; // Five minutes

    private static Logger LOG = LoggerFactory.getLogger(WebsocketEstimatedTimetableUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private GraphUpdaterManager updaterManager;

    /**
     * Url of the websocket server
     */
    private String url;

    /**
     * The ID for the static feed to which these TripUpdates are applied
     */
    private String feedId;

    /**
     * The number of seconds to wait before reconnecting after a failed connection.
     */
    private int reconnectPeriodSec;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        url = config.path("url").asText();
        feedId = config.path("feedId").asText("");
        reconnectPeriodSec = config.path("reconnectPeriodSec").asInt(DEFAULT_RECONNECT_PERIOD_SEC);

        blockReadinessUntilInitialized = config.path("blockReadinessUntilInitialized").asBoolean(false);
    }

    @Override
    public void setup() throws InterruptedException, ExecutionException {
        // Create a realtime data snapshot source and wait for runnable to be executed
        updaterManager.executeBlocking(new GraphWriterRunnable() {
            @Override
            public void run(Graph graph) {
                // Only create a realtime data snapshot source if none exists already
                if (graph.timetableSnapshotSource == null) {
                    TimetableSnapshotSource snapshotSource = new TimetableSnapshotSource(graph);
                    // Add snapshot source to graph
                    graph.timetableSnapshotSource = (snapshotSource);
                }
            }
        });
    }

    @Override
    public void run() throws InterruptedException {

        while (true) {
            WebSocketClient client = new WebSocketClient();
            SiriETSocketHandler socket = new SiriETSocketHandler();
            boolean connectionSuccessful = true;
            try {
                client.start();
                URI uri = new URI(url);

                ClientUpgradeRequest request = new ClientUpgradeRequest();

                client.connect(socket, uri, request);

            } catch (URISyntaxException e) {
                LOG.error("Could not connect to {}: {}", url, e.getCause().getMessage());
                connectionSuccessful = false;
            } catch (Exception e) {
                LOG.error("Unknown exception when trying to connect to {}:", url, e);
                connectionSuccessful = false;
            }

            if (!connectionSuccessful) {
                Thread.sleep(reconnectPeriodSec * 1000);
            }

            // Keep checking whether connection is still open
            while (true) {
                if (client == null || !socket.isConnected) {
                    // The connection is closed somehow, try to reconnect
                    if (connectionSuccessful) {
                        LOG.warn("Connection to {} was lost. Trying to reconnect...", url);
                    }
                    break;
                }
                Thread.sleep(CHECK_CONNECTION_PERIOD_SEC * 1000);
            }

            try {
                client.stop();
            } catch (Exception e) {
                // Ignore - shutting down
            }
        }
    }

    @Override
    public void teardown() {
    }


    private static transient final AtomicLong messageCounter = new AtomicLong(0);
    private static transient final AtomicLong updateCounter = new AtomicLong(0);
    private static transient final AtomicLong sizeCounter = new AtomicLong(0);
    private static transient long startTime;

    @WebSocket(maxTextMessageSize = 256 * 1024 * 1024, maxBinaryMessageSize = 256 * 1024 * 1024)
    public class SiriETSocketHandler {
        Logger LOG = LoggerFactory.getLogger(SiriETSocketHandler.class);

        boolean isConnected = true;

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            LOG.warn("Websocket closed: [{}, {}]", statusCode, reason);
            isConnected = false;
        }

        @OnWebSocketConnect
        public void onConnect(Session session) {
            LOG.info("Websocket connected to {}", session.getRemote().getInetSocketAddress());
            try {
                session.getRemote().sendString(SiriHelper.createETServiceRequestAsXml(session.toString()));
                isConnected = true;
                LOG.info("Websocket initialized");
            } catch (Exception e) {
                LOG.error("Unable to initialize Websocket");
                isConnected = false;
            } finally {
                startTime = System.currentTimeMillis();
            }
        }

        @OnWebSocketError
        public void onWebSocketError(Throwable throwable) {
            LOG.info("Websocket ERROR: " + throwable.getMessage());
            isConnected = false;
        }


        @OnWebSocketFrame
        public void onWebSocketFrame(Frame frame) {
            handleXml(new ByteBufferInputStream(frame.getPayload()));
        }

        private void handleXml(InputStream msg) {
            Siri siri = null;
            boolean fullDataset = false;
            try {


                final byte[] data = msg.readAllBytes();
                sizeCounter.addAndGet(data.length);

                final SiriType siriType = SiriType.parseFrom(data);
                siri = SiriMapper.map(siriType);

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (siri != null) {
                if (siri.getServiceDelivery() != null) {
                    // Handle trip updates via graph writer runnable
                    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries = siri.getServiceDelivery().getEstimatedTimetableDeliveries();

                    int numberOfUpdatedTrips = 0;
                    try {
                        numberOfUpdatedTrips = estimatedTimetableDeliveries.get(0).getEstimatedJourneyVersionFrames().get(0).getEstimatedVehicleJourneies().size();
                    } catch (Throwable t) {
                        //ignore
                    }
                    long numberOfMessages = messageCounter.incrementAndGet();
                    long numberOfUpdates = updateCounter.getAndAdd(numberOfUpdatedTrips);

                    if (numberOfMessages % 1000 == 0) {
                        LOG.info("Websocket stats: [messages: {},  updates: {}, total size: {}, current delay {} ms, time since startup: {}]",
                                numberOfMessages,
                                numberOfUpdates,
                                FileUtils.byteCountToDisplaySize(sizeCounter.get()),
                                (ZonedDateTime.now().toInstant().toEpochMilli() - siri.getServiceDelivery().getResponseTimestamp().toInstant().toEpochMilli()),
                                DurationFormatUtils.formatDuration((ZonedDateTime.now().toInstant().toEpochMilli() - startTime), "HH:mm:ss"));
                    }

                    EstimatedTimetableGraphWriterRunnable runnable =
                            new EstimatedTimetableGraphWriterRunnable(fullDataset,
                                    estimatedTimetableDeliveries);

                    updaterManager.execute(runnable);
                } else if (siri.getDataReadyNotification() != null) {
                    // NOT its intended use, but the current implementation sends a DataReadyNotification when initial delivery is complete.
                    LOG.info("WS initialized after {} ms - processed {} messages with {} updates and {} bytes",
                            (System.currentTimeMillis()-startTime),
                            messageCounter.get(),
                            updateCounter.get(),
                            FileUtils.byteCountToDisplaySize(sizeCounter.get()));
                    isInitialized = true;
                }
            }
        }
    }
}
