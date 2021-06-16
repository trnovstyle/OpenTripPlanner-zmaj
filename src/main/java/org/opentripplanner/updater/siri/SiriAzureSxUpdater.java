package org.opentripplanner.updater.siri;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.esotericsoftware.kryo.io.Input;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.SiriFuzzyTripMatcher;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.opentripplanner.util.HttpUtils;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SiriAzureSxUpdater extends AbstractAzureSiriUpdater {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private AlertsUpdateHandler updateHandler;
    private AlertPatchService alertPatchService;
    private static transient final AtomicLong messageCounter = new AtomicLong(0);
    private transient long startTime;

    /**
     * The SX messages need some configuration and specific parameters from router-config.json.
     * @param graph Reference to the Graph database instance.
     * @param config parameters from router-config.json
     */
    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        super.configure(graph, config);

        Preconditions.checkNotNull(config.path("feedId"), "'feedId' must be set");

        alertPatchService = new AlertPatchServiceImpl(graph);
        updateHandler = new AlertsUpdateHandler();
        updateHandler.setFeedId(config.path("feedId").asText());
        updateHandler.setAlertPatchService(alertPatchService);
        updateHandler.setSiriFuzzyTripMatcher(new SiriFuzzyTripMatcher(graph.index));
    }

    @Override
    protected void initializeData(String url, Consumer<ServiceBusReceivedMessageContext> consumer) throws IOException {
        if (url == null) {
            LOG.info("No history url set up for Siri Azure Sx Updater");
            return;
        }
        startTime = now();
        LOG.info("Fetching initial Siri SX data from {}", url);

        final long t1 = System.currentTimeMillis();
        final InputStream data = HttpUtils.getData(url, "Accept", "application/xml");
        final long t2 = System.currentTimeMillis();

        if (data == null) {
            throw new IOException("Historical endpoint returned no data from url" + url);
        }

        var reader = new InputStreamReader(data);
        var string = CharStreams.toString(reader);

        LOG.info("Fetching initial data - finished after {} ms, got {} bytes", (t2 -t1), string.length());

        processMessageBlocking(string, "SX-INITIAL-1");

        LOG.info("Azure Sx updated initialized after {} ms: [messages: {}, total size: {}, time since startup: {}]",
                (System.currentTimeMillis() - t2),
                messageCounter.get(),
                string.length(),
                DurationFormatUtils.formatDuration((now() - startTime), "HH:mm:ss"));
    }

    @Override
    protected void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
        var message = messageContext.getMessage();

        LOG.debug("Processing message. messageId={}, sequenceNumber={}, enqueued time={}",
                message.getMessageId(),
                message.getSequenceNumber(),
                message.getEnqueuedTime());

        messageCounter.incrementAndGet();
        try {
            var siri = SiriXml.parseXml(message.getBody().toString());
            if(siri.getServiceDelivery() == null
                    || siri.getServiceDelivery().getSituationExchangeDeliveries() == null
                    || siri.getServiceDelivery().getSituationExchangeDeliveries().isEmpty()) {

                LOG.warn("Empty Siri message for messageId {}", message.getMessageId());
                LOG.debug(message.getBody().toString());
                return;
            }

            updaterManager.execute(graph -> updateHandler.update(siri.getServiceDelivery()));
        } catch (JAXBException | XMLStreamException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private void processMessageBlocking(String message, String id) {
        messageCounter.incrementAndGet();
        try {
            var siri = SiriXml.parseXml(message);
            if(siri.getServiceDelivery() == null
                    || siri.getServiceDelivery().getSituationExchangeDeliveries() == null
                    || siri.getServiceDelivery().getSituationExchangeDeliveries().isEmpty()) {

                LOG.warn("Empty Siri message for messageId {}", id);
                LOG.debug(message);
                return;
            }

            updaterManager.executeBlocking(graph -> updateHandler.update(siri.getServiceDelivery()));
        } catch (JAXBException | XMLStreamException | ExecutionException | InterruptedException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    protected void errorConsumer(ServiceBusErrorContext errorContext) {
        defaultErrorConsumer(errorContext);
    }

    public AlertPatchService getAlertPatchService() {
        return alertPatchService;
    }

}
