# Azure Service Bus

Microsoft Azure Service Bus is a fully managed enterprise message broker with message queues and publish-subscribe topics [2].


# Azure Event Hub


Published events are removed from Event Hub based on a configurable time-based retention policy [1].

All Event Hubs consumers connect via the AMQP 1.0 session [1].

There can be at most 5 concurrent readers on a partition per consumer group; however it is recommended that 
there is only one active receiver on a partition per consumer group. Within a single partition, each reader receives all of the messages [1]. WTF!!!

Within a single partition, each reader receives all of the messages. If you have multiple readers on the same partition, then you process duplicate 
messages. You need to handle this in your code, which may not be trivial. However, it's a valid approach in some scenarios[1]. IS THIS NOT WHAT I WANT??

## Message Replay
This is one of the unique capabilities of Event Hubs. Since the messages are not removed from the stream by the receiver. The messages in the stream can be processed by simply adjusting the index that the receiver points to.
This capability is not available in Azure Service Bus. Since the messages are pulled out by the receiver, the message cannot be processed again. [3]


# Azure JMS


# Bibliography

1. [Azure Event Hubs Features]([https://docs.microsoft.com/sv-se/azure/event-hubs/event-hubs-features])

2. [Azure Service Bus](https://docs.microsoft.com/sv-se/azure/service-bus-messaging/)

3. [Serverless360 Azure Event Hub vs Service Bus](https://www.serverless360.com/blog/azure-event-hubs-vs-service-bus)