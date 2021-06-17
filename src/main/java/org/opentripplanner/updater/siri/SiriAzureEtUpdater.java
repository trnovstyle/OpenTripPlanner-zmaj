package org.opentripplanner.updater.siri;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.client.utils.URIBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.HttpUtils;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVersionFrameStructure;
import uk.org.siri.siri20.Siri;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SiriAzureEtUpdater extends AbstractAzureSiriUpdater {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private static transient final AtomicLong messageCounter = new AtomicLong(0);
    private LocalDate fromDateTime;

    private EstimatedTimetableGraphWriterRunnable getSiriRunnable(String message, String id) throws JAXBException, XMLStreamException {
        var siri = SiriXml.parseXml(message);
        if(siri.getServiceDelivery() == null
                || siri.getServiceDelivery().getEstimatedTimetableDeliveries() == null
                || siri.getServiceDelivery().getEstimatedTimetableDeliveries().isEmpty()) {

            LOG.warn("Empty Siri message {}: {}", id, message);
            return null;
        }

        var updates = siri.getServiceDelivery().getEstimatedTimetableDeliveries();
        return new EstimatedTimetableGraphWriterRunnable(false, updates);
    }

    private void processMessageBlocking(String message, String id) {
        try {
            EstimatedTimetableGraphWriterRunnable siriRunnable = getSiriRunnable(message, id);
            if (siriRunnable == null) {
                return;
            }
            updaterManager.executeBlocking(siriRunnable);
        } catch (JAXBException | XMLStreamException | InterruptedException | ExecutionException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private void processMessage(String message, String id) {
        try {
            EstimatedTimetableGraphWriterRunnable siriRunnable = getSiriRunnable(message, id);
            if (siriRunnable == null) {
                return;
            }

            updaterManager.execute(siriRunnable);
        } catch (JAXBException | XMLStreamException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void configure(Graph graph, JsonNode config) throws Exception {
        super.configure(graph, config);
        if (config.has("history")) {
            var historyNode = config.path("history");
            fromDateTime = asDateOrRelativePeriod(historyNode, "fromDateTime", "-P1D");
        }
    }

    @Override
    protected void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
        var message = messageContext.getMessage();
        messageCounter.incrementAndGet();

        if(messageCounter.get() % 100 == 0) {
            LOG.info("Total SIRI-ET messages received={}", messageCounter.get());
        }

        processMessage(message.getBody().toString(), message.getMessageId());

    }

    @Override
    protected void errorConsumer(ServiceBusErrorContext errorContext) {
        defaultErrorConsumer(errorContext);
    }

    @Override
    protected void initializeData(String url, Consumer<ServiceBusReceivedMessageContext> consumer) throws IOException, URISyntaxException {
        if (url == null) {
            LOG.info("No history url set up for Siri Azure ET Updater");
            return;
        }
        url = new URIBuilder(url)
                .addParameter("fromDateTime", fromDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .toString();
        long startTime = now();
        LOG.info("Fetching initial Siri ET data from {}", url);


        final long t1 = System.currentTimeMillis();
        final InputStream data = HttpUtils.getData(url, "Accept", "application/xml", timeout);
        final long t2 = System.currentTimeMillis();

        if (data == null) {
            throw new IOException("Historical endpoint returned no data from url" + url);
        }

        var reader = new InputStreamReader(data);
        var string = CharStreams.toString(reader);

        LOG.info("Fetching initial data - finished after {} ms, got {} bytes", (t2 -t1), string.length());

        processMessageBlocking(string, "ET-INITIAL-1");

        LOG.info("Azure ET updated initialized after {} ms: [messages: {}, total size: {}, time since startup: {}]",
                (System.currentTimeMillis() - t2),
                messageCounter.get(),
                string.length(),
                DurationFormatUtils.formatDuration((now() - startTime), "HH:mm:ss"));
    }
}
