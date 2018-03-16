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

package org.opentripplanner.routing.impl;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;

import java.util.*;
import java.util.Map.Entry;

public class AlertPatchServiceImpl implements AlertPatchService {

    private Graph graph;

    private Map<String, AlertPatch> alertPatches = new HashMap<>();
    private Map<AgencyAndId, Set<AlertPatch>> patchesByRoute = new HashMap<>();
    private Map<AgencyAndId, Set<AlertPatch>> patchesByStop = new HashMap<>();
    private Map<StopAndRouteOrTripKey, Set<AlertPatch>> patchesByStopAndRoute = new HashMap<>();
    private Map<StopAndRouteOrTripKey, Set<AlertPatch>> patchesByStopAndTrip = new HashMap<>();
    private Map<AgencyAndId, Set<AlertPatch>> patchesByTrip = new HashMap<>();
    private Map<String, Set<AlertPatch>> patchesByAgency = new HashMap<>();
    private Map<String, Set<AlertPatch>> patchesByTripPattern = new HashMap<>();

    public AlertPatchServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Collection<AlertPatch> getAllAlertPatches() {
        return alertPatches.values();
    }

    @Override
    public AlertPatch getPatchById(String id) {
        return alertPatches.get(id);
    }

    @Override
    public Collection<AlertPatch> getStopPatches(AgencyAndId stop) {
        Set<AlertPatch> result = patchesByStop.get(stop);
        if (result == null || result.isEmpty()) {
            // Search for alerts on parent-stop
            if (graph != null && graph.index != null) {
                Stop quay = graph.index.stopForId.get(stop);
                if (quay != null && quay.getParentStation() != null) {
                    result = patchesByStop.get(new AgencyAndId(stop.getAgencyId(), quay.getParentStation()));
                }
            }
        }
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getRoutePatches(AgencyAndId route) {
        Set<AlertPatch> result = patchesByRoute.get(route);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;

    }

    @Override
    public Collection<AlertPatch> getTripPatches(AgencyAndId trip) {
        Set<AlertPatch> result = patchesByTrip.get(trip);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }


    @Override
    public Collection<AlertPatch> getAgencyPatches(String agency) {
        Set<AlertPatch> result = patchesByAgency.get(agency);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndRoutePatches(AgencyAndId stop, AgencyAndId route) {
        Set<AlertPatch> result = patchesByStopAndRoute.get(new StopAndRouteOrTripKey(stop, route));
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndTripPatches(AgencyAndId stop, AgencyAndId trip) {
        Set<AlertPatch> result = patchesByStopAndTrip.get(new StopAndRouteOrTripKey(stop, trip));
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getTripPatternPatches(TripPattern pattern) {
        Set<AlertPatch> result = patchesByTripPattern.get(pattern.code);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public synchronized void apply(AlertPatch alertPatch) {
        if (alertPatches.containsKey(alertPatch.getId())) {
            expire(alertPatches.get(alertPatch.getId()));
        }

        alertPatch.apply(graph);
        alertPatches.put(alertPatch.getId(), alertPatch);

        AgencyAndId stop = alertPatch.getStop();
        AgencyAndId route = alertPatch.getRoute();
        AgencyAndId trip = alertPatch.getTrip();

        if (stop != null && trip != null) {
            addToMap(patchesByStopAndTrip, new StopAndRouteOrTripKey(stop, trip), alertPatch);
        } else if (stop != null && route != null) {
            addToMap(patchesByStopAndRoute, new StopAndRouteOrTripKey(stop, route), alertPatch);
        } else {
            if (stop != null) {
                addToMap(patchesByStop, stop, alertPatch);
            }

            if (route != null) {
                addToMap(patchesByRoute, route, alertPatch);
            }

            if (trip != null) {
                addToMap(patchesByTrip, trip, alertPatch);
            }
        }

        String agency = alertPatch.getAgency();
        if (agency != null && !agency.isEmpty()) {
            addToMap(patchesByAgency, agency, alertPatch);
        }

        List<TripPattern> tripPatterns = alertPatch.getTripPatterns();
        if (tripPatterns != null) {
            for (TripPattern pattern : tripPatterns) {
                addToMap(patchesByTripPattern, pattern.code, alertPatch);
            }
        }

    }

    private static void addToMap(Map map, AgencyAndId key, Object alertPatch) {
        Set set = (Set) map.getOrDefault(key, new HashSet());
        set.add(alertPatch);
        map.put(key, set);
    }

    private static void addToMap(Map map, String key, Object alertPatch) {
        Set set = (Set) map.getOrDefault(key, new HashSet());
        set.add(alertPatch);
        map.put(key, set);
    }


    private static void addToMap(Map map, StopAndRouteOrTripKey key, Object alertPatch) {
        Set set = (Set) map.getOrDefault(key, new HashSet());
        set.add(alertPatch);
        map.put(key, set);
    }

    @Override
    public void expire(Set<String> purge) {
        for (String patchId : purge) {
            if (alertPatches.containsKey(patchId)) {
                expire(alertPatches.get(patchId));
            }
        }

        alertPatches.keySet().removeAll(purge);
    }

    @Override
    public void expireAll() {
        for (AlertPatch alertPatch : alertPatches.values()) {
            expire(alertPatch);
        }
        alertPatches.clear();
    }

    @Override
    public void expireAllExcept(Set<String> retain) {
        ArrayList<String> toRemove = new ArrayList<String>();

        for (Entry<String, AlertPatch> entry : alertPatches.entrySet()) {
            final String key = entry.getKey();
            if (!retain.contains(key)) {
                toRemove.add(key);
                expire(entry.getValue());
            }
        }
        alertPatches.keySet().removeAll(toRemove);
    }

    private void expire(AlertPatch alertPatch) {
        AgencyAndId stop = alertPatch.getStop();
        AgencyAndId route = alertPatch.getRoute();
        AgencyAndId trip = alertPatch.getTrip();

        if (stop != null) {
            removeAlertPatch(patchesByStop, stop, alertPatch);
        }

        if (route != null) {
            removeAlertPatch(patchesByRoute, route, alertPatch);
        }

        if (trip != null) {
            removeAlertPatch(patchesByTrip, trip, alertPatch);
        }

        if (stop != null && route != null) {
            removeAlertPatch(patchesByStopAndRoute, new StopAndRouteOrTripKey(stop, route), alertPatch);
        }

        if (stop != null && trip != null) {
            removeAlertPatch(patchesByStopAndTrip, new StopAndRouteOrTripKey(stop, trip), alertPatch);
        }

        String agency = alertPatch.getAgency();
        if (agency != null) {
            removeAlertPatch(patchesByAgency, agency, alertPatch);
        }

        List<TripPattern> tripPatterns = alertPatch.getTripPatterns();
        if (tripPatterns != null) {
            for (TripPattern pattern : tripPatterns) {
                removeAlertPatch(patchesByTripPattern, pattern, alertPatch);
            }
        }
        alertPatch.remove(graph);
    }

    private void removeAlertPatch(Map map, Object key, AlertPatch alertPatch) {
        Set alertPatches = (Set) map.get(key);
        if (alertPatches != null) {
            alertPatches.remove(alertPatch);
        }
    }

    private class StopAndRouteOrTripKey {
        private final AgencyAndId stop;
        private final AgencyAndId routeOrTrip;

        public StopAndRouteOrTripKey(AgencyAndId stop, AgencyAndId routeOrTrip) {
            this.stop = stop;
            this.routeOrTrip = routeOrTrip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StopAndRouteOrTripKey that = (StopAndRouteOrTripKey) o;

            if (!stop.equals(that.stop)) {
                return false;
            }
            return routeOrTrip.equals(that.routeOrTrip);
        }

        @Override
        public int hashCode() {
            int result = stop.hashCode();
            result = 31 * result + routeOrTrip.hashCode();
            return result;
        }
    }
}
