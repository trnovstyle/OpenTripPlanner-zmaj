/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.alerts;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.*;
import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.SiriFuzzyTripMatcher;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This updater only includes GTFS-Realtime Service Alert feeds.
 * @author novalis
 *
 */
public class AlertsUpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(AlertsUpdateHandler.class);

    private String feedId;

    private Set<String> patchIds = new HashSet<String>();

    private AlertPatchService alertPatchService;

    /** How long before the posted start of an event it should be displayed to users */
    private long earlyStart;

    /** Set only if we should attempt to match the trip_id from other data in TripDescriptor */
    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;


    private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

    public void update(FeedMessage message) {
        alertPatchService.expire(patchIds);
        patchIds.clear();

        for (FeedEntity entity : message.getEntityList()) {
            if (!entity.hasAlert()) {
                continue;
            }
            GtfsRealtime.Alert alert = entity.getAlert();
            String id = entity.getId();
            handleAlert(id, alert);
        }
    }

    public void update(ServiceDelivery delivery) {
        for (SituationExchangeDeliveryStructure sxDelivery : delivery.getSituationExchangeDeliveries()) {
            SituationExchangeDeliveryStructure.Situations situations = sxDelivery.getSituations();
            if (situations != null) {
                long t1 = System.currentTimeMillis();
                Set<String> idsToExpire = new HashSet<>();
                Set<AlertPatch> alertPatches = new HashSet<>();

                for (PtSituationElement sxElement : situations.getPtSituationElements()) {
                    Pair<Set<String>, Set<AlertPatch>> alertPatchChangePair = handleAlert(sxElement);
                    if (alertPatchChangePair != null) {
                        idsToExpire.addAll(alertPatchChangePair.getLeft());
                        alertPatches.addAll(alertPatchChangePair.getRight());
                    }
                }

                alertPatchService.expire(idsToExpire);
                alertPatchService.applyAll(alertPatches);
                log.info("Added {} alerts, expired {} alerts based on {} situations, current alert-count: {}, elapsed time {}ms", alertPatches.size(), idsToExpire.size(), situations.getPtSituationElements().size(), alertPatchService.getAllAlertPatches().size(), (System.currentTimeMillis()-t1));
            }
        }
    }

    private Pair<Set<String>, Set<AlertPatch>> handleAlert(PtSituationElement situation) {
        Alert alert = new Alert();

        alert.alertDescriptionText = getTranslatedString(situation.getDescriptions());
        alert.alertDetailText = getTranslatedString(situation.getDetails());
        alert.alertHeaderText = getTranslatedString(situation.getSummaries());

        alert.alertUrl = getInfoLinkAsString(situation.getInfoLinks());

        Set<String> idsToExpire = new HashSet<>();
        boolean expireSituation = (situation.getProgress() != null &&
                situation.getProgress().equals(WorkflowStatusEnumeration.CLOSED));

        //ROR-54
        if (!expireSituation && //If situation is closed, it must be allowed - it will remove already existing alerts
                ((alert.alertHeaderText == null || alert.alertHeaderText.toString().isEmpty()) &&
                (alert.alertDescriptionText == null || alert.alertDescriptionText.toString().isEmpty()) &&
                (alert.alertDetailText == null || alert.alertDetailText.toString().isEmpty()))) {
            log.debug("Empty Alert - ignoring situationNumber: {}", situation.getSituationNumber() != null ? situation.getSituationNumber().getValue():null);
            return null;
        }

        ArrayList<TimePeriod> periods = new ArrayList<>();
        if(situation.getValidityPeriods().size() > 0) {
            long bestStartTime = Long.MAX_VALUE;
            long bestEndTime = 0;
            for (HalfOpenTimestampOutputRangeStructure activePeriod : situation.getValidityPeriods()) {

                final long realStart = activePeriod.getStartTime() != null ? activePeriod.getStartTime().toInstant().toEpochMilli() : 0;
                final long start = activePeriod.getStartTime() != null? realStart - earlyStart : 0;
                if (realStart > 0 && realStart < bestStartTime) {
                    bestStartTime = realStart;
                }

                final long realEnd = activePeriod.getEndTime() != null ? activePeriod.getEndTime().toInstant().toEpochMilli() : 0;
                final long end = activePeriod.getEndTime() != null? realEnd  : 0;
                if (realEnd > 0 && realEnd > bestEndTime) {
                    bestEndTime = realEnd;
                }

                periods.add(new TimePeriod(start, end));
            }
            if (bestStartTime != Long.MAX_VALUE) {
                alert.effectiveStartDate = new Date(bestStartTime);
            }
            if (bestEndTime != 0) {
                alert.effectiveEndDate = new Date(bestEndTime);
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }

        String situationNumber;

        if (situation.getSituationNumber() != null) {
            situationNumber = situation.getSituationNumber().getValue();
        } else {
            situationNumber = null;
        }

        String paddedSituationNumber = situationNumber + ":";

        Set<AlertPatch> patches = new HashSet<>();
        AffectsScopeStructure affectsStructure = situation.getAffects();

        if (affectsStructure != null) {

            AffectsScopeStructure.Operators operators = affectsStructure.getOperators();

            if (operators != null && !isListNullOrEmpty(operators.getAffectedOperators())) {
                for (AffectedOperatorStructure affectedOperator : operators.getAffectedOperators()) {

                    OperatorRefStructure operatorRef = affectedOperator.getOperatorRef();
                    if (operatorRef == null || operatorRef.getValue() == null) {
                        continue;
                    }

                    // SIRI Operators are mapped to OTP Agency, this i probably wrong - but
                    // I leave this for now.
                    String agencyId = operatorRef.getValue();

                    String id = paddedSituationNumber + agencyId;
                    if (expireSituation) {
                        idsToExpire.add(id);
                    } else {
                        AlertPatch alertPatch = new AlertPatch();
                        alertPatch.setAgencyId(agencyId);
                        alertPatch.setTimePeriods(periods);
                        alertPatch.setAlert(alert);
                        alertPatch.setId(id);
                        patches.add(alertPatch);
                    }
                }
            }

            AffectsScopeStructure.Networks networks = affectsStructure.getNetworks();
            Set<Route> stopRoutes = new HashSet<>();
            if (networks != null) {
                for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                    List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                    if (affectedLines != null && !isListNullOrEmpty(affectedLines)) {
                        for (AffectedLineStructure line : affectedLines) {

                            LineRef lineRef = line.getLineRef();

                            if (lineRef == null || lineRef.getValue() == null) {
                                continue;
                            }

                            stopRoutes.addAll(siriFuzzyTripMatcher.getRoutes(lineRef.getValue()));
                        }
                    }
                }
            }

            AffectsScopeStructure.StopPoints stopPoints = affectsStructure.getStopPoints();
            AffectsScopeStructure.StopPlaces stopPlaces = affectsStructure.getStopPlaces();

            if (stopPoints != null && !isListNullOrEmpty(stopPoints.getAffectedStopPoints())) {

                for (AffectedStopPointStructure stopPoint : stopPoints.getAffectedStopPoints()) {
                    StopPointRef stopPointRef = stopPoint.getStopPointRef();
                    if (stopPointRef == null || stopPointRef.getValue() == null) {
                        continue;
                    }

                    AgencyAndId stopId = siriFuzzyTripMatcher.getStop(stopPointRef.getValue());

                    String id = paddedSituationNumber + stopPointRef.getValue();
                    if (expireSituation) {
                        idsToExpire.add(id);
                    } else {
                        if (stopId != null) {
                            if (stopRoutes.isEmpty()) {
                                AlertPatch stopOnlyAlertPatch = new AlertPatch();
                                stopOnlyAlertPatch.setStop(stopId);
                                stopOnlyAlertPatch.setTimePeriods(periods);
                                stopOnlyAlertPatch.setId(id);

                                updateStopConditions(stopOnlyAlertPatch, stopPoint.getStopConditions());

                                patches.add(stopOnlyAlertPatch);
                            } else {
                                //Adding combination of stop & route
                                for (Route route : stopRoutes) {
                                    id = paddedSituationNumber + stopPointRef.getValue() + "-" + route.getId().getId();

                                    AlertPatch alertPatch = new AlertPatch();
                                    alertPatch.setStop(stopId);
                                    alertPatch.setRoute(route.getId());
                                    alertPatch.setTimePeriods(periods);
                                    alertPatch.setId(id);

                                    updateStopConditions(alertPatch, stopPoint.getStopConditions());

                                    patches.add(alertPatch);
                                }
                            }
                        }
                    }
                }
            } else if (stopPlaces != null && !isListNullOrEmpty(stopPlaces.getAffectedStopPlaces())) {

                for (AffectedStopPlaceStructure stopPoint : stopPlaces.getAffectedStopPlaces()) {
                    StopPlaceRef stopPlace = stopPoint.getStopPlaceRef();
                    if (stopPlace == null || stopPlace.getValue() == null) {
                        continue;
                    }

                    AgencyAndId stopId = siriFuzzyTripMatcher.getStop(stopPlace.getValue());

                    String id = paddedSituationNumber + stopPlace.getValue();
                    if (expireSituation) {
                        idsToExpire.add(id);
                    } else {
                        if (stopId != null) {

                            if (stopRoutes.isEmpty()) {
                                AlertPatch stopOnlyAlertPatch = new AlertPatch();
                                stopOnlyAlertPatch.setStop(stopId);
                                stopOnlyAlertPatch.setTimePeriods(periods);
                                stopOnlyAlertPatch.setId(id);
                                patches.add(stopOnlyAlertPatch);
                            } else {
                                //Adding combination of stop & route
                                for (Route route : stopRoutes) {
                                    id = paddedSituationNumber + stopPlace.getValue() + "-" + route.getId().getId();

                                    AlertPatch alertPatch = new AlertPatch();
                                    alertPatch.setStop(stopId);
                                    alertPatch.setRoute(route.getId());
                                    alertPatch.setTimePeriods(periods);
                                    alertPatch.setId(id);
                                    patches.add(alertPatch);
                                }
                            }
                        }
                    }
                }
            } else if (networks != null && !isListNullOrEmpty(networks.getAffectedNetworks())) {

                for (AffectsScopeStructure.Networks.AffectedNetwork affectedNetwork : networks.getAffectedNetworks()) {
                    List<AffectedLineStructure> affectedLines = affectedNetwork.getAffectedLines();
                    if (affectedLines != null && !isListNullOrEmpty(affectedLines)) {
                        for (AffectedLineStructure line : affectedLines) {

                            LineRef lineRef = line.getLineRef();

                            if (lineRef == null || lineRef.getValue() == null) {
                                continue;
                            }

                            Set<Route> affectedRoutes = siriFuzzyTripMatcher.getRoutes(lineRef.getValue());
                            for (Route route : affectedRoutes) {

                                String id = paddedSituationNumber + route.getId();
                                if (expireSituation) {
                                    idsToExpire.add(id);
                                } else {
                                    AlertPatch alertPatch = new AlertPatch();
                                    alertPatch.setRoute(route.getId());
                                    alertPatch.setTimePeriods(periods);
                                    alertPatch.setId(id);
                                    patches.add(alertPatch);
                                }
                            }
                        }
                    }
                    NetworkRefStructure networkRef = affectedNetwork.getNetworkRef();
                    if (networkRef == null || networkRef.getValue() == null) {
                        continue;
                    }
                    String networkId = networkRef.getValue();

                    String id = paddedSituationNumber + networkId;
                    if (expireSituation) {
                        idsToExpire.add(id);
                    } else {
                        AlertPatch alertPatch = new AlertPatch();
                        alertPatch.setId(id);
                        alertPatch.setTimePeriods(periods);
                        patches.add(alertPatch);
                    }
                }
            }

            AffectsScopeStructure.VehicleJourneys vjs = affectsStructure.getVehicleJourneys();
            if (vjs != null && !isListNullOrEmpty(vjs.getAffectedVehicleJourneies())) {

                for (AffectedVehicleJourneyStructure affectedVehicleJourney : vjs.getAffectedVehicleJourneies()) {

                    String lineRef = null;
                    if (affectedVehicleJourney.getLineRef() != null) {
                        lineRef = affectedVehicleJourney.getLineRef().getValue();
                    }

                    List<AffectedStopPointStructure> affectedStops = new ArrayList<>();

                    List<AffectedRouteStructure> routes = affectedVehicleJourney.getRoutes();
                    // Resolve AffectedStop-ids
                    if (routes != null) {
                        for (AffectedRouteStructure route : routes) {
                            if (route.getStopPoints() != null) {
                                List<Serializable> stopPointsList = route.getStopPoints().getAffectedStopPointsAndLinkProjectionToNextStopPoints();
                                for (Serializable serializable : stopPointsList) {
                                    if (serializable instanceof AffectedStopPointStructure) {
                                        AffectedStopPointStructure stopPointStructure = (AffectedStopPointStructure) serializable;
                                        affectedStops.add(stopPointStructure);
                                    }
                                }
                            }
                        }
                    }

                    List<VehicleJourneyRef> vehicleJourneyReves = affectedVehicleJourney.getVehicleJourneyReves();

                    ZonedDateTime originAimedDepartureTime = (affectedVehicleJourney.getOriginAimedDepartureTime() != null ? affectedVehicleJourney.getOriginAimedDepartureTime():ZonedDateTime.now());

                    ServiceDate serviceDate = new ServiceDate(originAimedDepartureTime.getYear(), originAimedDepartureTime.getMonthValue(), originAimedDepartureTime.getDayOfMonth());

                    ServiceDate yesterday = new ServiceDate().previous();

                    if (!isListNullOrEmpty(vehicleJourneyReves)) {
                        if (serviceDate.compareTo(yesterday) >= 0 ) {
                            for (VehicleJourneyRef vehicleJourneyRef : vehicleJourneyReves) {

                                AgencyAndId tripId = siriFuzzyTripMatcher.getTripId(vehicleJourneyRef.getValue());
                                if (tripId == null) {
                                    tripId = siriFuzzyTripMatcher.getTripIdForTripShortNameServiceDateAndMode(vehicleJourneyRef.getValue(),
                                            serviceDate,
                                            TraverseMode.RAIL);
                                }
                                if (tripId != null) {
                                    if (! affectedStops.isEmpty()) {
                                        for (AffectedStopPointStructure affectedStop : affectedStops) {
                                            AgencyAndId stop = siriFuzzyTripMatcher.getStop(affectedStop.getStopPointRef().getValue());
                                            if (stop == null) {
                                                continue;
                                            }
                                            String id = paddedSituationNumber + tripId.getId() + "-" + stop.getId();
                                            if (expireSituation) {
                                                idsToExpire.add(id);
                                            } else {
                                                AlertPatch alertPatch = new AlertPatch();
                                                alertPatch.setTrip(tripId);
                                                alertPatch.setStop(stop);
                                                alertPatch.setId(id);

                                                updateStopConditions(alertPatch, affectedStop.getStopConditions());

                                                //  A tripId for a given date may be reused for other dates not affected by this alert.
                                                List<TimePeriod> timePeriodList = new ArrayList<>();
                                                timePeriodList.add(new TimePeriod(originAimedDepartureTime.toEpochSecond() * 1000, originAimedDepartureTime.plusDays(1).toEpochSecond() * 1000));
                                                alertPatch.setTimePeriods(timePeriodList);


                                                Alert vehicleJourneyAlert = new Alert();
                                                vehicleJourneyAlert.alertHeaderText = alert.alertHeaderText;
                                                vehicleJourneyAlert.alertDescriptionText = alert.alertDescriptionText;
                                                vehicleJourneyAlert.alertDetailText = alert.alertDetailText;
                                                vehicleJourneyAlert.alertUrl = alert.alertUrl;
                                                vehicleJourneyAlert.effectiveStartDate = serviceDate.getAsDate();
                                                vehicleJourneyAlert.effectiveEndDate = serviceDate.next().getAsDate();

                                                alertPatch.setAlert(vehicleJourneyAlert);

                                                patches.add(alertPatch);
                                            }
                                        }
                                    } else {
                                        String id = paddedSituationNumber + tripId.getId();
                                        if (expireSituation) {
                                            idsToExpire.add(id);
                                        } else {
                                            AlertPatch alertPatch = new AlertPatch();
                                            alertPatch.setTrip(tripId);
                                            alertPatch.setId(id);

                                            //  A tripId for a given date may be reused for other dates not affected by this alert.
                                            List<TimePeriod> timePeriodList = new ArrayList<>();
                                            timePeriodList.add(new TimePeriod(originAimedDepartureTime.toEpochSecond() * 1000, originAimedDepartureTime.plusDays(1).toEpochSecond() * 1000));
                                            alertPatch.setTimePeriods(timePeriodList);


                                            Alert vehicleJourneyAlert = new Alert();
                                            vehicleJourneyAlert.alertHeaderText = alert.alertHeaderText;
                                            vehicleJourneyAlert.alertDescriptionText = alert.alertDescriptionText;
                                            vehicleJourneyAlert.alertDetailText = alert.alertDetailText;
                                            vehicleJourneyAlert.alertUrl = alert.alertUrl;
                                            vehicleJourneyAlert.effectiveStartDate = serviceDate.getAsDate();
                                            vehicleJourneyAlert.effectiveEndDate = serviceDate.next().getAsDate();

                                            alertPatch.setAlert(vehicleJourneyAlert);

                                            patches.add(alertPatch);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (lineRef != null) {

                        Set<Route> affectedRoutes = siriFuzzyTripMatcher.getRoutes(lineRef);
                        for (Route route : affectedRoutes) {
                            String id = paddedSituationNumber + route.getId();
                            if (expireSituation) {
                                idsToExpire.add(id);
                            } else {
                                AlertPatch alertPatch = new AlertPatch();
                                alertPatch.setRoute(route.getId());
                                alertPatch.setTimePeriods(periods);
                                alertPatch.setId(id);
                                patches.add(alertPatch);
                            }
                        }
                    }
                }
            }
        }

        // Alerts are not partially updated - cancel ALL current related alerts before adding updated.
        idsToExpire.addAll(alertPatchService.getAllAlertPatches()
            .stream()
            .filter(alertPatch -> alertPatch.getSituationNumber().equals(situationNumber))
            .map(alertPatch -> alertPatch.getId())
            .collect(Collectors.toList()));

        if (!patches.isEmpty() | !idsToExpire.isEmpty()) {

            for (AlertPatch patch : patches) {
                if (patch.getAlert() == null) {
                    patch.setAlert(alert);
                }
                if (patch.getStopConditions().isEmpty()) {
                    updateStopConditions(patch, null);
                }
                patch.getAlert().alertType = situation.getReportType();

                if (situation.getSeverity() != null) {
                    patch.getAlert().severity = situation.getSeverity().value();
                } else {
                    // When severity is not set - use default
                    patch.getAlert().severity = SeverityEnumeration.NORMAL.value();
                }

                if (situation.getParticipantRef() != null) {
                    String codespace = situation.getParticipantRef().getValue();
                    patch.setFeedId(codespace + ":Authority:" + codespace); //TODO: Should probably not assume this codespace -> authority rule
                }

                patch.setSituationNumber(situationNumber);
            }
        } else if (expireSituation) {
            log.debug("Expired non-existing alert - ignoring situation with situationNumber {}", situationNumber);
        } else {
            log.info("No match found for Alert - ignoring situation with situationNumber {}", situationNumber);
        }

        return Pair.of(idsToExpire, patches);
    }

    private I18NString getInfoLinkAsString(PtSituationElement.InfoLinks infoLinks) {
        if (infoLinks != null) {
            if (!isListNullOrEmpty(infoLinks.getInfoLinks())) {
                InfoLinkStructure infoLinkStructure = infoLinks.getInfoLinks().get(0);
                if (infoLinkStructure != null && infoLinkStructure.getUri() != null) {
                    return new NonLocalizedString(infoLinkStructure.getUri());
                }
            }
        }
        return null;
    }

    private void updateStopConditions(AlertPatch alertPatch, List<RoutePointTypeEnumeration> stopConditions) {
        Set<StopCondition> alertStopConditions = new HashSet<>();
        if (stopConditions != null) {
            for (RoutePointTypeEnumeration stopCondition : stopConditions) {
                switch (stopCondition) {
                    case EXCEPTIONAL_STOP:
                        alertStopConditions.add(StopCondition.EXCEPTIONAL_STOP);
                        break;
                    case DESTINATION:
                        alertStopConditions.add(StopCondition.DESTINATION);
                        break;
                    case NOT_STOPPING:
                        alertStopConditions.add(StopCondition.NOT_STOPPING);
                        break;
                    case REQUEST_STOP:
                        alertStopConditions.add(StopCondition.REQUEST_STOP);
                        break;
                    case START_POINT:
                        alertStopConditions.add(StopCondition.START_POINT);
                        break;
                }
            }
        }
        if (alertStopConditions.isEmpty()) {
            //No StopConditions are set - set default
            alertStopConditions.add(StopCondition.START_POINT);
            alertStopConditions.add(StopCondition.DESTINATION);

        }
        alertPatch.getStopConditions().addAll(alertStopConditions);
    }

    private boolean isListNullOrEmpty(List list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return false;
    }


    private void handleAlert(String id, GtfsRealtime.Alert alert) {
        Alert alertText = new Alert();
        alertText.alertDescriptionText = deBuffer(alert.getDescriptionText());
        alertText.alertHeaderText = deBuffer(alert.getHeaderText());
        alertText.alertUrl = deBuffer(alert.getUrl());
        ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
        if(alert.getActivePeriodCount() > 0) {
            long bestStartTime = Long.MAX_VALUE;
            long lastEndTime = Long.MIN_VALUE;
            for (TimeRange activePeriod : alert.getActivePeriodList()) {
                final long realStart = activePeriod.hasStart() ? activePeriod.getStart() : 0;
                final long start = activePeriod.hasStart() ? realStart - earlyStart : 0;
                if (realStart > 0 && realStart < bestStartTime) {
                    bestStartTime = realStart;
                }
                final long end = activePeriod.hasEnd() ? activePeriod.getEnd() : Long.MAX_VALUE;
                if (end > lastEndTime) {
                    lastEndTime = end;
                }
                periods.add(new TimePeriod(start, end));
            }
            if (bestStartTime != Long.MAX_VALUE) {
                alertText.effectiveStartDate = new Date(bestStartTime * 1000);
            }
            if (lastEndTime != Long.MIN_VALUE) {
                alertText.effectiveEndDate = new Date(lastEndTime * 1000);
            }
        } else {
            // Per the GTFS-rt spec, if an alert has no TimeRanges, than it should always be shown.
            periods.add(new TimePeriod(0, Long.MAX_VALUE));
        }
        for (EntitySelector informed : alert.getInformedEntityList()) {
            if (fuzzyTripMatcher != null && informed.hasTrip()) {
                TripDescriptor trip = fuzzyTripMatcher.match(feedId, informed.getTrip());
                informed = informed.toBuilder().setTrip(trip).build();
            }
            String patchId = createId(id, informed);

            String routeId = null;
            if (informed.hasRouteId()) {
                routeId = informed.getRouteId();
            }

            int direction;
            if (informed.hasTrip() && informed.getTrip().hasDirectionId()) {
                direction = informed.getTrip().getDirectionId();
            } else {
                direction = -1;
            }

            // TODO: The other elements of a TripDescriptor are ignored...
            String tripId = null;
            if (informed.hasTrip() && informed.getTrip().hasTripId()) {
                tripId = informed.getTrip().getTripId();
            }
            String stopId = null;
            if (informed.hasStopId()) {
                stopId = informed.getStopId();
            }

            String agencyId = informed.getAgencyId();
            if (informed.hasAgencyId()) {
                agencyId = informed.getAgencyId().intern();
            }

            AlertPatch patch = new AlertPatch();
            patch.setFeedId(feedId);
            if (routeId != null) {
                patch.setRoute(new AgencyAndId(feedId, routeId));
                // Makes no sense to set direction if we don't have a route
                if (direction != -1) {
                    patch.setDirectionId(direction);
                }
            }
            if (tripId != null) {
                patch.setTrip(new AgencyAndId(feedId, tripId));
            }
            if (stopId != null) {
                patch.setStop(new AgencyAndId(feedId, stopId));
            }
            if (agencyId != null && routeId == null && tripId == null && stopId == null) {
                patch.setAgencyId(agencyId);
            }
            patch.setTimePeriods(periods);
            patch.setAlert(alertText);

            patch.setId(patchId);
            patchIds.add(patchId);

            alertPatchService.apply(patch);
        }
    }

    private String createId(String id, EntitySelector informed) {
        return id + " "
            + (informed.hasAgencyId  () ? informed.getAgencyId  () : " null ") + " "
            + (informed.hasRouteId   () ? informed.getRouteId   () : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasDirectionId() ?
                informed.getTrip().hasDirectionId() : " null ") + " "
            + (informed.hasRouteType () ? informed.getRouteType () : " null ") + " "
            + (informed.hasStopId    () ? informed.getStopId    () : " null ") + " "
            + (informed.hasTrip() && informed.getTrip().hasTripId() ?
                informed.getTrip().getTripId() : " null ");
    }

    /**
     * convert a protobuf TranslatedString to a OTP TranslatedString
     *
     * @return A TranslatedString containing the same information as the input
     */
    private I18NString deBuffer(GtfsRealtime.TranslatedString input) {
        Map<String, String> translations = new HashMap<>();
        for (GtfsRealtime.TranslatedString.Translation translation : input.getTranslationList()) {
            String language = translation.getLanguage();
            String string = translation.getText();
            translations.put(language, string);
        }
        return translations.isEmpty() ? null : TranslatedString.getI18NString(translations);
    }

    /**
     * convert a SIRI DefaultedTextStructure to a OTP TranslatedString
     *
     * @return A TranslatedString containing the same information as the input
     * @param input
     */
    private I18NString getTranslatedString(List<DefaultedTextStructure> input) {
        Map<String, String> translations = new HashMap<>();
        if (input != null && input.size() > 0) {
            for (DefaultedTextStructure textStructure : input) {
                String language = "";
                String value = "";
                if (textStructure.getLang() != null) {
                    language = textStructure.getLang();
                }
                if (textStructure.getValue() != null) {
                    value = textStructure.getValue();
                }
                translations.put(language, value);
            }
        } else {
            translations.put("", "");
        }

        return translations.isEmpty() ? null : TranslatedString.getI18NString(translations);
    }

    public void setFeedId(String feedId) {
        if(feedId != null)
            this.feedId = feedId.intern();
    }

    public void setAlertPatchService(AlertPatchService alertPatchService) {
        this.alertPatchService = alertPatchService;
    }

    public long getEarlyStart() {
        return earlyStart;
    }

    public void setEarlyStart(long earlyStart) {
        this.earlyStart = earlyStart;
    }

    public void setFuzzyTripMatcher(GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher) {
        this.fuzzyTripMatcher = fuzzyTripMatcher;
    }

    public void setSiriFuzzyTripMatcher(SiriFuzzyTripMatcher siriFuzzyTripMatcher) {
        this.siriFuzzyTripMatcher = siriFuzzyTripMatcher;
    }
}
