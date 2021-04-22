# Azure Service Bus updator
OpenTripPlanner supports receiving SIRI ET/SX messages[[1]](#1). The purpose of this documentation is to describe how to configure OpenTripPlanner to receive messages from Azure Service Bus.

Microsoft Azure Service Bus is a fully managed enterprise message broker with message queues and publish-subscribe topics [[2]](#2).

# Configuration
To enable the SIRI updator you can subscribe to Azure Service Bus ET/SX message topics. The types in OpenTripPlanner are *siri-azure-et-updater* and *siri-azure-sx-updater*.

The configuration should be added to the *updators* section of the *router-config.json* OpenTripPlanner configuration file.

In the example below *<TOPIC XX>* should be substituted with the topic name existing in the Azure Service Bus. Further the *<ServiceBusUrl>* should be substituted with endpoint information and maybe an access key.

````
{
        "updaters": [
                {

                        "type": "siri-azure-et-updater",
                        "topic": "<TOPIC ET>",
                        "servicebus-url": "<ServiceBusUrl>"
                },
                {

                        "type": "siri-azure-sx-updater",
                        "topic": "<TOPIC SX>",
                        "feedId": "ST",
                        "servicebus-url": "<ServiceBusUrl>"
                }
        ]
}
````

# Bibliography

<a id="1">[1]</a>
Norweigian SIRI profile
- https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370420/Norwegian+SIRI+profile

<a id="2">[2]</a>
Azure Service Bus Messaging documentation 
- https://docs.microsoft.com/en-us/azure/service-bus-messaging
