package org.opentripplanner.netex.mapping;

import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.OtpTransitDaoBuilder;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    TransportTypeMapper transportTypeMapper = new TransportTypeMapper();
    AgencyMapper agencyMapper = new AgencyMapper();

    public org.opentripplanner.model.Route mapRoute(Line line, OtpTransitDaoBuilder transitBuilder, NetexDao netexDao, String timeZone){

        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();

        if (line.getOperatorRef() == null) {
            // Get agencies from serviceJourneys
            Collection<String> serviceJourneys = netexDao.getServiceJourneyById().values().stream()
                    .flatMap(s -> s.stream()).filter(t -> t.getLineRef() != null && t.getLineRef().getValue().getRef().equals(line.getId()))
                    .map(t -> t.getOperatorRef().getRef()).distinct().collect(Collectors.toList());

            Collection<Operator> operators = netexDao.getOperators().values().stream()
                    .filter(a -> serviceJourneys.contains(a.getId())).collect(Collectors.toList());

            Agency compositeAgency = agencyMapper.combineAndMapAgency(operators, timeZone);
            otpRoute.setAgency(compositeAgency);
        } else {
            String agencyId = line.getOperatorRef().getRef();
            Agency agency = transitBuilder.getAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        }
        otpRoute.setId(AgencyAndIdFactory.getAgencyAndId(line.getId()));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportTypeMapper.mapTransportType(line.getTransportMode().value()));

        return otpRoute;
    }
}