package org.opentripplanner.netex.mapping;

import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.impl.OtpTransitDaoBuilder;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.ServiceJourneyPattern;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;

public class NetexMapper {

    final OtpTransitDaoBuilder transitBuilder;

    AgencyMapper agencyMapper = new AgencyMapper();

    RouteMapper routeMapper = new RouteMapper();

    StopMapper stopMapper = new StopMapper();

    TripPatternMapper tripPatternMapper = new TripPatternMapper();

    NoticeMapper noticeMapper = new NoticeMapper();

    NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper();

    TransferMapper transferMapper = new TransferMapper();

    String agencyId;

    public NetexMapper(OtpTransitDaoBuilder transitBuilder, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;
    }

    public OtpTransitDaoBuilder mapNetexToOtp(NetexDao netexDao) {
        AgencyAndIdFactory.setAgencyId(agencyId);

        for (Operator operator : netexDao.getOperators().values()) {
            if (operator != null) {
                transitBuilder.getAgencies().add(agencyMapper.mapAgency(operator, netexDao.getTimeZone()));
            }
        }

        for (Line line : netexDao.getLineById().values()) {
            if (line != null) {
                Route route = routeMapper.mapRoute(line, transitBuilder, netexDao, netexDao.getTimeZone());
                transitBuilder.getRoutes().add(route);
            }
        }

        for (StopPlace stopPlace : netexDao.getMultimodalStopPlaceById().values()) {
            if (stopPlace != null) {
                Stop stop = stopMapper.mapMultiModalStop(stopPlace);
                transitBuilder.getMultiModalStops().add(stop);
                transitBuilder.getStops().add(stop);
            }
        }

        for (String stopPlaceId : netexDao.getStopsById().keySet()) {
            Collection<StopPlace> stopPlaceAllVersions = netexDao.getStopsById().get(stopPlaceId);
            if (stopPlaceAllVersions != null) {
                Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions, transitBuilder, netexDao);
                for (Stop stop : stops) {
                    transitBuilder.getStops().add(stop);
                }
            }
        }

        for (JourneyPattern journeyPattern : netexDao.getJourneyPatternsById().values()) {
            if (journeyPattern != null) {
                tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexDao);
            }
        }

        for (String serviceId : netexDao.getServiceIds().values()) {
            transitBuilder.getCalendarDates().addAll(mapToCalendarDates(AgencyAndIdFactory.getAgencyAndId(serviceId), netexDao));
        }

        for (org.rutebanken.netex.model.ServiceJourneyInterchange interchange : netexDao.getInterchanges().values()) {
            if (interchange != null) {
                Transfer transfer = transferMapper.mapTransfer(interchange, transitBuilder, netexDao);
                if (transfer != null) {
                    transitBuilder.getTransfers().add(transfer);
                }
            }
        }
        for (Notice notice : netexDao.getNoticeMap().values()) {
            if (notice != null) {
                org.opentripplanner.model.Notice otpNotice = noticeMapper.mapNotice(notice);
                transitBuilder.getNoticesById().add(otpNotice);
            }
        }

        for (org.rutebanken.netex.model.NoticeAssignment noticeAssignment : netexDao
                .getNoticeAssignmentMap().values()) {
            if (noticeAssignment != null) {
                Collection<NoticeAssignment> otpNoticeAssignments = noticeAssignmentMapper.mapNoticeAssignment(noticeAssignment, netexDao);
                for (org.opentripplanner.model.NoticeAssignment otpNoticeAssignment : otpNoticeAssignments){
                transitBuilder.getNoticeAssignmentsById().add( otpNoticeAssignment);}
            }
        }

        return transitBuilder;
    }
}
