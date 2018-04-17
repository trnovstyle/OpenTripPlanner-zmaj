package org.opentripplanner.netex.mapping;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServiceLinkMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    private Collection<ShapePoint> mapServiceLink(ServiceLink serviceLink, JourneyPattern journeyPattern, MutableInt sequenceCounter, MutableDouble distanceCounter) {
        Collection<ShapePoint> shapePoints = new ArrayList<>();

        if (serviceLink.getProjections() == null || serviceLink.getProjections().getProjectionRefOrProjection() == null) {
            LOG.warn("Ignore service link without projection: " + serviceLink);
        } else {
            for (JAXBElement<?> projectionElement : serviceLink.getProjections().getProjectionRefOrProjection()) {
                Object projectionObj = projectionElement.getValue();
                if (projectionObj instanceof LinkSequenceProjection_VersionStructure) {
                    LinkSequenceProjection_VersionStructure linkSequenceProjection = (LinkSequenceProjection_VersionStructure) projectionObj;
                    if (linkSequenceProjection.getLineString() != null) {
                        List<Double> coordinates = linkSequenceProjection.getLineString().getPosList().getValue();
                        double distance = serviceLink.getDistance() != null ? serviceLink.getDistance().doubleValue() : -1;
                        for (int i = 0; i < coordinates.size(); i += 2) {
                            ShapePoint shapePoint = new ShapePoint();
                            shapePoint.setShapeId(createShapePointIdFromJourneyPatternId(AgencyAndIdFactory.createAgencyAndId(journeyPattern.getId())));
                            shapePoint.setLat(coordinates.get(i));
                            shapePoint.setLon(coordinates.get(i + 1));
                            shapePoint.setSequence(sequenceCounter.toInteger());
                            if (distance != -1) {
                                shapePoint.setDistTraveled(distanceCounter.doubleValue() + (distance / (coordinates.size() / 2) * (i / 2)));
                            }
                            sequenceCounter.increment();
                            shapePoints.add(shapePoint);
                        }
                        distanceCounter.add(distance != -1 ? distance : 0);
                    } else {
                        LOG.warn("Ignore linkSequenceProjection without linestring for: " + linkSequenceProjection.toString());
                    }
                }
            }
        }

        return shapePoints;
    }

    public Collection<ShapePoint> getShapePointsByJourneyPattern(JourneyPattern journeyPattern, NetexDao netexDao) {
        Collection<ShapePoint> shapePoints = new ArrayList<>();
        if (journeyPattern.getLinksInSequence() != null) {
            MutableInt sequenceCounter = new MutableInt(0);
            MutableDouble distance = new MutableDouble(0);
            for (LinkInLinkSequence_VersionedChildStructure linkInLinkSequence_versionedChildStructure
                    : journeyPattern.getLinksInSequence().getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern()) {

                String serviceLinkRef = ((ServiceLinkInJourneyPattern_VersionedChildStructure) linkInLinkSequence_versionedChildStructure).getServiceLinkRef().getRef();
                for (ShapePoint shapePoint : mapServiceLink(netexDao.serviceLinkById.lookup(serviceLinkRef),
                        journeyPattern, sequenceCounter, distance)) {
                    shapePoints.add(shapePoint);
                }
            }
        }
        return shapePoints;
    }

    private AgencyAndId createShapePointIdFromJourneyPatternId(AgencyAndId journeyPatternId) {
        AgencyAndId id = new AgencyAndId(journeyPatternId.getAgencyId(), journeyPatternId.getId().replace("JourneyPattern", "ServiceLink"));
        return id;
    }
}
