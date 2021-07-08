package org.opentripplanner.updater.siri;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.esotericsoftware.kryo.io.Input;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.client.utils.URIBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.SiriFuzzyTripMatcher;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.opentripplanner.util.HttpUtils;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SiriAzureSxUpdater extends AbstractAzureSiriUpdater {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private AlertsUpdateHandler updateHandler;
    private AlertPatchService alertPatchService;
    private static transient final AtomicLong messageCounter = new AtomicLong(0);
    private LocalDate publishFromDateTime;
    private LocalDate publishToDateTime;

    private void processMessage(String message, String id) {
        try {
            Siri siri = getSiri(message, id);
            if (siri == null) {
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
            Siri siri = getSiri(message, id);
            if (siri == null) {
                return;
            }

            updaterManager.executeBlocking(graph -> updateHandler.update(siri.getServiceDelivery()));
        } catch (JAXBException | XMLStreamException | ExecutionException | InterruptedException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private Siri getSiri(String message, String id) throws JAXBException, XMLStreamException {
        var siri = SiriXml.parseXml(message);
        if (siri.getServiceDelivery() == null
                || siri.getServiceDelivery().getSituationExchangeDeliveries() == null
                || siri.getServiceDelivery().getSituationExchangeDeliveries().isEmpty()) {

            LOG.warn("Empty Siri message for messageId {}", id);
            LOG.debug(message);
            return null;
        }
        return siri;
    }

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

        if (config.has("history")) {
            var historyNode = config.path("history");
            publishFromDateTime = asDateOrRelativePeriod(historyNode, "publishFromDateTime", "-P1D");
            publishToDateTime = asDateOrRelativePeriod(historyNode, "publishToDateTime", "P1D");
        }

    }

    @Override
    protected void initializeData(String url, Consumer<ServiceBusReceivedMessageContext> consumer) throws IOException, URISyntaxException {
        if (url == null) {
            LOG.info("No history url set up for Siri Azure Sx Updater");
            return;
        }
        long startTime = now();
        url = new URIBuilder(url)
                .addParameter("publishFromDateTime", publishFromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .addParameter("publishToDateTime", publishToDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .toString();
        LOG.info("Fetching initial Siri SX data from {}", url);

        final long t1 = System.currentTimeMillis();
        final InputStream data = HttpUtils.getData(url, "Accept", "application/xml", timeout);
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
        processMessage(message.getBody().toString(), message.getMessageId());
    }

    @Override
    protected void errorConsumer(ServiceBusErrorContext errorContext) {
        defaultErrorConsumer(errorContext);
    }

    public AlertPatchService getAlertPatchService() {
        return alertPatchService;
    }

}
