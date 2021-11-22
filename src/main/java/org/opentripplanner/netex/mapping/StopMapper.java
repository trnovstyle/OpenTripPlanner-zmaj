package org.opentripplanner.netex.mapping;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.modes.TransitModeService;
import org.opentripplanner.netex.issues.QuayWithoutCoordinates;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

class StopMapper {

  private final TransportModeMapper transportModeMapper;

  private final DataImportIssueStore issueStore;

  private final FeedScopedIdFactory idFactory;

  StopMapper(
      FeedScopedIdFactory idFactory,
      DataImportIssueStore issueStore,
      TransitModeService transitModeService
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.transportModeMapper = new TransportModeMapper(transitModeService);
  }

  /**
   * Map Netex Quay to OTP Stop
   */
  @Nullable
  Stop mapQuayToStop(
          Quay quay,
          Station parentStation,
          Collection<FareZone> fareZones,
          StopPlace stopPlace
  ) {
    WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(quay.getCentroid());

    if (coordinate == null) {
      issueStore.add(new QuayWithoutCoordinates(quay.getId()));
      return null;
    }

    Stop stop = new Stop(
        idFactory.createId(quay.getId()),
        parentStation.getName(),
        quay.getPublicCode(),
        quay.getDescription() != null ? quay.getDescription().getValue() : null,
        WgsCoordinateMapper.mapToDomain(quay.getCentroid()),
        null,
        null,
        null,
        fareZones,
        null,
        null,
        transportModeMapper.map(stopPlace)
    );
    stop.setParentStation(parentStation);

    return stop;
  }
}
