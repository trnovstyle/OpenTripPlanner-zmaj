package org.opentripplanner.netex.mapping;

import org.apache.http.auth.AUTH;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.OtpTransitDaoBuilder;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    TransportTypeMapper transportTypeMapper = new TransportTypeMapper();
    AgencyMapper agencyMapper = new AgencyMapper();

    public org.opentripplanner.model.Route mapRoute(Line line, OtpTransitDaoBuilder transitBuilder, NetexDao netexDao, String timeZone){

        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();
        Network network = netexDao.getNetworkByLineId().get(line.getId());
        GroupOfLines groupOfLines = netexDao.getGroupOfLinesByLineId().get(line.getId());

        Agency agency = null;

        if (network != null && netexDao.getAuthoritiesByNetworkId().get(network.getId()) != null) {
            Authority authority = netexDao.getAuthoritiesByNetworkId().get(network.getId());
            String agencyId = authority.getId();
            agency = transitBuilder.getAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        } else if (groupOfLines != null && netexDao.getAuthoritiesByGroupOfLinesId().get(groupOfLines.getId()) != null) {
            Authority authority = netexDao.getAuthoritiesByGroupOfLinesId().get(groupOfLines.getId());
            String agencyId = authority.getId();
            agency = transitBuilder.getAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        } else {
            LOG.warn("No authority found for " + line.getId());
            agency = agencyMapper.getDefaultAgency(timeZone);
            String agencyId = agency.getId();
            if (!transitBuilder.getAgencies().stream().anyMatch(a -> a.getId().equals(agencyId))) {
                transitBuilder.getAgencies().add(agency);
            }
        }

        otpRoute.setAgency(agency);

        otpRoute.setId(AgencyAndIdFactory.getAgencyAndId(line.getId()));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportTypeMapper.mapTransportType(line.getTransportMode().value()));

        return otpRoute;
    }
}