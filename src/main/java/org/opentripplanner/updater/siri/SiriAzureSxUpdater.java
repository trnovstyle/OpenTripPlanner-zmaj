package org.opentripplanner.updater.siri;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.SiriFuzzyTripMatcher;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

public class SiriAzureSxUpdater extends AbstractAzureSiriUpdater {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private AlertsUpdateHandler updateHandler;
    private AlertPatchService alertPatchService;

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
    void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
        var message = messageContext.getMessage();

        LOG.info("Processing message. Session={}, Sequence={}", message.getMessageId(), message.getSequenceNumber());

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

    @Override
    void errorConsumer(ServiceBusErrorContext errorContext) {
        defaultErrorConsumer(errorContext);
    }

    public AlertPatchService getAlertPatchService() {
        return alertPatchService;
    }

}
