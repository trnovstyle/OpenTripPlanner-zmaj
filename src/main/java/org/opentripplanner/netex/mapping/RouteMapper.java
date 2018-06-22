package org.opentripplanner.netex.mapping;

import org.apache.commons.collections.CollectionUtils;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BookingArrangement;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.PresentationStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.stream.Collectors;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    private TransportModeMapper transportModeMapper = new TransportModeMapper();
    private AuthorityToAgencyMapper authorityToAgencyMapper = new AuthorityToAgencyMapper();
    private HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
    private KeyValueMapper keyValueMapper = new KeyValueMapper();
    private ContactStructureMapper contactStructureMapper = new ContactStructureMapper();

    public org.opentripplanner.model.Route mapRoute(Line_VersionStructure line, OtpTransitBuilder transitBuilder, NetexDao netexDao, String timeZone) {

        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();

        otpRoute.setId(AgencyAndIdFactory.createAgencyAndId(line.getId()));
        otpRoute.setAgency(findAuthorityForRoute(transitBuilder, netexDao, line, timeZone));
        otpRoute.setOperator(findLineOperator(line, transitBuilder));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportModeMapper.getTransportMode(line.getTransportMode(), line.getTransportSubmode()));
        otpRoute.setTransportSubmode(transportModeMapper.getTransportSubmode(line.getTransportSubmode()));
        otpRoute.setKeyValues(keyValueMapper.mapKeyValues(line.getKeyList()));
        // Temp fix
        if (otpRoute.getShortName() == null)
            otpRoute.setShortName("");

        if (line.getPresentation() != null) {
            PresentationStructure presentation = line.getPresentation();
            if (presentation.getColour() != null) {
                otpRoute.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
            }
            if (presentation.getTextColour() != null) {
                otpRoute.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
            }
        }

        if (line instanceof FlexibleLine){
            mapFlexibleLineProperties((FlexibleLine) line, otpRoute);
        }

        return otpRoute;
    }

    private void mapFlexibleLineProperties(FlexibleLine flexibleLine, Route otpRoute) {
        if (flexibleLine.getFlexibleLineType() != null) {
            otpRoute.setFlexibleRouteType(Route.FlexibleRouteTypeEnum.valueOf(flexibleLine.getFlexibleLineType().value()));
        }

        BookingArrangement otpBookingArrangement = new BookingArrangement();

        otpBookingArrangement.setBookingContact(contactStructureMapper.mapContactStructure(flexibleLine.getBookingContact()));
        if (flexibleLine.getBookingNote() != null) {
            otpBookingArrangement.setBookingNote(flexibleLine.getBookingNote().getValue());
        }
        if(flexibleLine.getBookWhen()!=null) {
            otpBookingArrangement.setBookWhen(BookingArrangement.PurchaseWhenEnum.valueOf(flexibleLine.getBookWhen().value()));
        }
        if(flexibleLine.getBookingAccess()!=null) {
            otpBookingArrangement.setBookingAccess(BookingArrangement.BookingAccessEnum.valueOf(flexibleLine.getBookingAccess().value()));
        }
        if (!CollectionUtils.isEmpty(flexibleLine.getBuyWhen())) {
            otpBookingArrangement.setBuyWhen(flexibleLine.getBuyWhen().stream().map(bw -> BookingArrangement.PurchaseMomentEnum.valueOf(bw.value())).collect(Collectors.toList()));
        }
        if (!CollectionUtils.isEmpty(flexibleLine.getBookingMethods())) {
            otpBookingArrangement.setBookingMethods(flexibleLine.getBookingMethods().stream().map(bm -> BookingArrangement.BookingMethodEnum.valueOf(bm.value())).collect(Collectors.toList()));
        }
        otpBookingArrangement.setLatestBookingTime(flexibleLine.getLatestBookingTime());
        otpBookingArrangement.setMinimumBookingPeriod(flexibleLine.getMinimumBookingPeriod());
        otpRoute.setBookingArrangements(otpBookingArrangement);

    }

    private Operator findLineOperator(Line_VersionStructure line, OtpTransitBuilder transitBuilder) {
        OperatorRefStructure opeRef = line.getOperatorRef();

        if(opeRef != null) {
            return transitBuilder.getOperatorsById().get(AgencyAndIdFactory.createAgencyAndId(opeRef.getRef()));

        }
        return null;
    }

    private Agency findAuthorityForRoute(OtpTransitBuilder transitBuilder, NetexDao netexDao, Line_VersionStructure line, String timeZone) {
        Agency otpAgency = null;
        Network network = netexDao.networkByLineId.lookup(line.getId());
        GroupOfLines groupOfLines = netexDao.groupOfLinesByLineId.lookup(line.getId());

        if (network != null && netexDao.authoritiesByNetworkId.lookup(network.getId()) != null) {
            Authority authority = netexDao.authoritiesByNetworkId.lookup(network.getId());
            String agencyId = authority.getId();
            otpAgency = transitBuilder.findAgencyById(agencyId);
        } else if (groupOfLines != null && netexDao.authoritiesByGroupOfLinesId.lookup(groupOfLines.getId()) != null) {
            Authority authority = netexDao.authoritiesByGroupOfLinesId.lookup(groupOfLines.getId());
            String agencyId = authority.getId();
            otpAgency = transitBuilder.findAgencyById(agencyId);
        }

        return otpAgency != null ? otpAgency : getDefaultAgency(transitBuilder, line, timeZone);
    }

    private Agency getDefaultAgency(OtpTransitBuilder transitBuilder, Line_VersionStructure line, String timeZone) {
        LOG.warn("No authority found for " + line.getId());
        Agency agency = authorityToAgencyMapper.getDefaultAgency(timeZone);

        if (transitBuilder.findAgencyById(agency.getId()) == null) {
            transitBuilder.getAgencies().add(agency);
        }
        return agency;
    }
}