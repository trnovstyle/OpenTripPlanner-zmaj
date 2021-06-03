package org.opentripplanner.updater.siri;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

public class SiriAzureEtUpdater extends AbstractAzureSiriUpdater {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private int messageCounter;

    @Override
    protected void messageConsumer(ServiceBusReceivedMessageContext messageContext) {
        var message = messageContext.getMessage();
        messageCounter++;

        if(messageCounter % 100 == 0) {
            LOG.info("Total SIRI-ET messages received={}", messageCounter);
        }

        try {
            var siri = SiriXml.parseXml(message.getBody().toString());
            if(siri.getServiceDelivery() == null
                    || siri.getServiceDelivery().getEstimatedTimetableDeliveries() == null
                    || siri.getServiceDelivery().getEstimatedTimetableDeliveries().isEmpty()) {

                LOG.warn("Empty Siri message: {}", message.getBody().toString());
                return;
            }

            var updates = siri.getServiceDelivery().getEstimatedTimetableDeliveries();
            var runnable = new EstimatedTimetableGraphWriterRunnable(false, updates);

            updaterManager.execute(runnable);
        } catch (JAXBException | XMLStreamException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

    }

    @Override
    protected void errorConsumer(ServiceBusErrorContext errorContext) {
        defaultErrorConsumer(errorContext);
    }

}
