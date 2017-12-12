package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.model.Transfer;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;

public class NetexMapper {

    private final AgencyMapper agencyMapper = new AgencyMapper();

    private final NoticeMapper noticeMapper = new NoticeMapper();

    private final NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper();

    private final RouteMapper routeMapper = new RouteMapper();

    private final StopMapper stopMapper = new StopMapper();

    private final TripPatternMapper tripPatternMapper = new TripPatternMapper();

    private final OtpTransitBuilder transitBuilder;

    private final TransferMapper transferMapper = new TransferMapper();

    private final String agencyId;


    public NetexMapper(OtpTransitBuilder transitBuilder, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;
    }

    public void mapNetexToOtp(NetexDao netexDao) {
        AgencyAndIdFactory.setAgencyId(agencyId);

        for (Authority authority : netexDao.getAuthorities()) {
            transitBuilder.getAgencies().add(agencyMapper.mapAgency(authority, netexDao.getTimeZone()));
        }

        for (Line line : netexDao.getLines()) {
            Route route = routeMapper.mapRoute(line, transitBuilder, netexDao, netexDao.getTimeZone());
            transitBuilder.getRoutes().add(route);
        }

        for (StopPlace stopPlace : netexDao.getMultimodalStops()) {
            if (stopPlace != null) {
                Stop stop = stopMapper.mapMultiModalStop(stopPlace);
                transitBuilder.getMultiModalStops().add(stop);
                transitBuilder.getStops().add(stop);
            }
        }

        for (String stopPlaceId : netexDao.getStopPlaceIds()) {
            Collection<StopPlace> stopPlaceAllVersions = netexDao.lookupStopPlacesById(stopPlaceId);
            if (stopPlaceAllVersions != null) {
                Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions, transitBuilder, netexDao);
                for (Stop stop : stops) {
                    transitBuilder.getStops().add(stop);
                }
            }
        }

        for (JourneyPattern journeyPattern : netexDao.getJourneyPatterns()) {
            tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexDao);
        }

        for (String serviceId : netexDao.getCalendarServiceIds()) {
            transitBuilder.getCalendarDates().addAll(mapToCalendarDates(AgencyAndIdFactory.createAgencyAndId(serviceId), netexDao));
        }

        for (org.rutebanken.netex.model.ServiceJourneyInterchange interchange : netexDao.getInterchanges()) {
            if (interchange != null) {
                Transfer transfer = transferMapper.mapTransfer(interchange, transitBuilder, netexDao);
                if (transfer != null) {
                    transitBuilder.getTransfers().add(transfer);
                }
            }
        }

        for (Notice notice : netexDao.getNotices()) {
            org.opentripplanner.model.Notice otpNotice = noticeMapper.mapNotice(notice);
            transitBuilder.getNoticesById().add(otpNotice);
        }

        for (org.rutebanken.netex.model.NoticeAssignment noticeAssignment : netexDao.getNoticeAssignments()) {
            Collection<NoticeAssignment> otpNoticeAssignments = noticeAssignmentMapper.mapNoticeAssignment(noticeAssignment, netexDao);
            for (NoticeAssignment otpNoticeAssignment : otpNoticeAssignments){
            transitBuilder.getNoticeAssignmentsById().add( otpNoticeAssignment);}
        }
    }
}
