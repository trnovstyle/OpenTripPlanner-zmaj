package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.NoticeAssignment;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.model.Transfer;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;

public class NetexMapper {

    private final AuthorityToAgencyMapper authorityToAgencyMapper = new AuthorityToAgencyMapper();

    private final NoticeMapper noticeMapper = new NoticeMapper();

    private final NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper();

    private final RouteMapper routeMapper = new RouteMapper();

    private final StopMapper stopMapper = new StopMapper();

    private final TripPatternMapper tripPatternMapper = new TripPatternMapper();

    private final ParkingMapper parkingMapper = new ParkingMapper();

    private final OtpTransitBuilder transitBuilder;

    private final OperatorMapper operatorMapper = new OperatorMapper();

    private final TariffZoneMapper tariffZoneMapper = new TariffZoneMapper();

    private final TransferMapper transferMapper = new TransferMapper();

    private final String agencyId;


    public NetexMapper(OtpTransitBuilder transitBuilder, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;
    }

    public void mapNetexToOtp(NetexDao netexDao) {
        AgencyAndIdFactory.setAgencyId(agencyId);

        for (Authority authority : netexDao.authoritiesById.values()) {
            transitBuilder.getAgencies().add(authorityToAgencyMapper.mapAgency(authority, netexDao.getTimeZone()));
        }

        for (org.rutebanken.netex.model.Operator operator : netexDao.operatorsById.values()) {
            transitBuilder.getOperatorsById().add(operatorMapper.map(operator));
        }

        for (Line line : netexDao.lineById.values()) {
            Route route = routeMapper.mapRoute(line, transitBuilder, netexDao, netexDao.getTimeZone());
            transitBuilder.getRoutes().add(route);
        }

        for (TariffZone tariffZone : netexDao.tariffZoneById.values()) {
            if (tariffZone != null) {
                org.opentripplanner.model.TariffZone otpTariffZone = tariffZoneMapper.mapTariffZone(tariffZone);
                transitBuilder.getTariffZones().add(otpTariffZone);
            }
        }

        for (StopPlace stopPlace : netexDao.multimodalStopPlaceById.values()) {
            if (stopPlace != null) {
                Stop stop = stopMapper.mapMultiModalStop(stopPlace);
                transitBuilder.getMultiModalStops().add(stop);
                transitBuilder.getStops().add(stop);
            }
        }

        for (String stopPlaceId : netexDao.stopPlaceById.keys()) {
            Collection<StopPlace> stopPlaceAllVersions = netexDao.stopPlaceById.lookup(stopPlaceId);
            if (stopPlaceAllVersions != null) {
                Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions, transitBuilder, netexDao);
                for (Stop stop : stops) {
                    transitBuilder.getStops().add(stop);
                }
            }
        }

        for (GroupOfStopPlaces group : netexDao.groupsOfStopPlacesById.values()) {
            if (group != null) {
                Stop stop = stopMapper.mapGroupsOfStopPlaces(group, transitBuilder.getStopByGroupOfStopPlaces(), transitBuilder.getStops());
                transitBuilder.getGroupsOfStopPlaces().add(stop);
                transitBuilder.getStops().add(stop);
            }
        }

        for (JourneyPattern journeyPattern : netexDao.journeyPatternsById.values()) {
            tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexDao);
        }

        for (String serviceId : netexDao.getCalendarServiceIds()) {
            transitBuilder.getCalendarDates().addAll(mapToCalendarDates(AgencyAndIdFactory.createAgencyAndId(serviceId), netexDao));
        }

        for (String parkingId : netexDao.parkingById.keys()) {
            transitBuilder.getParkings().add(parkingMapper.mapParking(netexDao.parkingById.lookupLastVersionById(parkingId)));
        }

        for (org.rutebanken.netex.model.ServiceJourneyInterchange interchange : netexDao.interchanges.values()) {
            if (interchange != null) {
                Transfer transfer = transferMapper.mapTransfer(interchange, transitBuilder, netexDao);
                if (transfer != null) {
                    transitBuilder.getTransfers().add(transfer);
                }
            }
        }

        for (Notice notice : netexDao.noticeById.values()) {
            org.opentripplanner.model.Notice otpNotice = noticeMapper.mapNotice(notice);
            transitBuilder.getNoticesById().add(otpNotice);
        }

        for (org.rutebanken.netex.model.NoticeAssignment noticeAssignment : netexDao.noticeAssignmentById.values()) {
            Collection<NoticeAssignment> otpNoticeAssignments = noticeAssignmentMapper.mapNoticeAssignment(noticeAssignment, netexDao);
            for (NoticeAssignment otpNoticeAssignment : otpNoticeAssignments){
            transitBuilder.getNoticeAssignmentsById().add( otpNoticeAssignment);}
        }
    }
}
