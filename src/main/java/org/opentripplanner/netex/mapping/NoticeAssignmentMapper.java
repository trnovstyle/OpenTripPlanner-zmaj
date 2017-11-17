package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class NoticeAssignmentMapper {

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    public Collection<NoticeAssignment> mapNoticeAssignment(org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment, NetexDao netexDao){
        Collection<NoticeAssignment> noticeAssignments = new ArrayList<>();
        String journeyPatternRef = netexNoticeAssignment.getNoticedObjectRef().getRef();

        if (getObjectType(netexNoticeAssignment).equals("StopPointInJourneyPattern")) {
            JourneyPattern journeyPattern = netexDao.lookupJourneyPatternByStopPointId(journeyPatternRef);

            if (journeyPattern != null && netexDao.serviceJourneysExist(journeyPattern.getId())) {
                // Map notice from StopPointInJourneyPattern to corresponding TimeTabledPassingTimes
                for (ServiceJourney serviceJourney : netexDao.lookupServiceJourneysById(journeyPattern.getId())) {
                    org.opentripplanner.model.NoticeAssignment otpNoticeAssignment = new org.opentripplanner.model.NoticeAssignment();

                    otpNoticeAssignment.setId(AgencyAndIdFactory.createAgencyAndId(netexNoticeAssignment.getId()));
                    otpNoticeAssignment.setNoticeId(AgencyAndIdFactory.createAgencyAndId(netexNoticeAssignment.getNoticeRef().getRef()));
                    otpNoticeAssignment.setElementId(AgencyAndIdFactory.createAgencyAndId(serviceJourney.getId()));

                    noticeAssignments.add(otpNoticeAssignment);
                }
            }
            else {
                LOG.warn("JourneyPattern " + journeyPatternRef + " not found when mapping notices.");
            }
        } else {
            org.opentripplanner.model.NoticeAssignment otpNoticeAssignment = new org.opentripplanner.model.NoticeAssignment();

            otpNoticeAssignment.setId(AgencyAndIdFactory.createAgencyAndId(netexNoticeAssignment.getId()));
            otpNoticeAssignment.setNoticeId(AgencyAndIdFactory.createAgencyAndId(netexNoticeAssignment.getNoticeRef().getRef()));
            otpNoticeAssignment.setElementId(AgencyAndIdFactory.createAgencyAndId(journeyPatternRef));

            noticeAssignments.add(otpNoticeAssignment);
        }

        return noticeAssignments;
    }

    private String getObjectType (org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment) {
        String objectType = "";
        String[] journeyPatternRefArray = netexNoticeAssignment.getNoticedObjectRef().getRef().split(":");

        if (netexNoticeAssignment.getNoticedObjectRef() != null && journeyPatternRefArray.length >= 2) {
            objectType = journeyPatternRefArray[1];
        }
        return objectType;
    }
}