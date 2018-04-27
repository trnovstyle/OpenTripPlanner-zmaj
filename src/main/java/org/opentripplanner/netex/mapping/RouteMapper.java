package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.PresentationStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    private TransportModeMapper transportModeMapper = new TransportModeMapper();
    private AuthorityToAgencyMapper authorityToAgencyMapper = new AuthorityToAgencyMapper();
    private HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
    private KeyValueMapper keyValueMapper = new KeyValueMapper();

    public org.opentripplanner.model.Route mapRoute(Line line, OtpTransitBuilder transitBuilder, NetexDao netexDao, String timeZone) {

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

        return otpRoute;
    }

    private Operator findLineOperator(Line line, OtpTransitBuilder transitBuilder) {
        OperatorRefStructure opeRef = line.getOperatorRef();

        if(opeRef != null) {
            return transitBuilder.getOperatorsById().get(AgencyAndIdFactory.createAgencyAndId(opeRef.getRef()));

        }
        return null;
    }

    private Agency findAuthorityForRoute(OtpTransitBuilder transitBuilder, NetexDao netexDao, Line line, String timeZone) {
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

    private Agency getDefaultAgency(OtpTransitBuilder transitBuilder, Line line, String timeZone) {
        LOG.warn("No authority found for " + line.getId());
        Agency agency = authorityToAgencyMapper.getDefaultAgency(timeZone);

        if (transitBuilder.findAgencyById(agency.getId()) == null) {
            transitBuilder.getAgencies().add(agency);
        }
        return agency;
    }
}