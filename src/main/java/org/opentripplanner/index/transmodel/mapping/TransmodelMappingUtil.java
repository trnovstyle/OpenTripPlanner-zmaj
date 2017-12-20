package org.opentripplanner.index.transmodel.mapping;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility methods for mapping transmddel API values to and from internal formats.
 */
public class TransmodelMappingUtil {

    private static final String LIST_VALUE_SEPARATOR = ",";

    private static final String GTFS_LIBRARY_ID_SEPARATOR = ":";

    private String fixedAgencyId;

    private Map<String, TraverseMode> traverseModeMap;

    public TransmodelMappingUtil(String fixedAgencyId, Map<String, TraverseMode> traverseModeMap) {
        this.fixedAgencyId = fixedAgencyId;
        this.traverseModeMap = traverseModeMap;
    }


    public String toIdString(AgencyAndId agencyAndId) {
        if (fixedAgencyId != null) {
            return agencyAndId.getId();
        }
        return GtfsLibrary.convertIdToString(agencyAndId);
    }

    public AgencyAndId fromIdString(String id) {
        if (fixedAgencyId != null) {
            return new AgencyAndId(fixedAgencyId, id);
        }
        return GtfsLibrary.convertIdFromString(id);
    }


    /**
     * Add agency id prefix to vertexIds if fixed agency is set.
     */
    public String preparePlaceRef(String input) {
        if (fixedAgencyId != null && input != null) {
            GenericLocation location = GenericLocation.fromOldStyleString(input);

            if (location.hasVertexId()) {
                String prefixedPlace = prepareAgencyAndId(location.place, GTFS_LIBRARY_ID_SEPARATOR);
                return new GenericLocation(location.name, prefixedPlace).toString();
            }

        }
        return input;
    }

    public String prepareListOfAgencyAndId(List<String> ids) {
        return mapCollectionOfValues(ids, this::prepareAgencyAndId);
    }

    public String prepareListOfAgencyAndId(List<String> ids, String separator) {
        return mapCollectionOfValues(ids, value -> prepareAgencyAndId(value, separator));
    }

    public String prepareAgencyAndId(String id) {
        return prepareAgencyAndId(id, null);
    }

    public String prepareAgencyAndId(String id, String separator) {
        if (fixedAgencyId != null && id != null) {
            return separator == null
                           ? fixedAgencyId + AgencyAndId.ID_SEPARATOR + id
                           : fixedAgencyId + separator + id;
        }
        return id;
    }

    /**
     * Convert a comma separated list of transmodel mode values into a corresponding comma separated list of otp modes.
     */
    public String mapListOfModes(List<String> transmodelModes) {
        return mapCollectionOfValues(transmodelModes, this::mapMode);
    }


    public String mapCollectionOfValues(Collection<String> values, Function<String, String> mapElementFunction) {
        if (values == null) {
            return null;
        }
        List<String> otpModelModes = values.stream().map(value -> mapElementFunction.apply(value)).collect(Collectors.toList());

        return Joiner.on(LIST_VALUE_SEPARATOR).join(otpModelModes);
    }

    public String mapMode(String transmodelMode) {
        TraverseMode traverseMode = traverseModeMap.get(transmodelMode.trim());
        if (traverseMode == null) {
            throw new IllegalArgumentException("Mode not supported: " + transmodelMode);
        }
        return traverseMode.name();
    }

    // Create a dummy route to be able to reuse GtfsLibrary functionality
    public Object mapVehicleTypeToTraverseMode(int vehicleType) {
        Route dummyRoute = new Route();
        dummyRoute.setType(vehicleType);
        try {
            return GtfsLibrary.getTraverseMode(dummyRoute);
        } catch (IllegalArgumentException iae) {
            return "unknown";
        }
    }


    public Long serviceDateToSecondsSinceEpoch(ServiceDate serviceDate) {
        if (serviceDate == null) {
            return null;
        }
        return serviceDate.getAsDate().getTime() / 1000;
    }

    public ServiceDate secondsSinceEpochToServiceDate(Long secondsSinceEpoch) {
        if (secondsSinceEpoch == null) {
            return new ServiceDate();
        }
        return new ServiceDate(new Date(secondsSinceEpoch * 1000));
    }
}
