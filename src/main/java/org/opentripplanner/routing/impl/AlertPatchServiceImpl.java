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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class AlertPatchServiceImpl implements AlertPatchService {

    private Graph graph;

    private Map<String, AlertPatch> alertPatches = new ConcurrentHashMap<>();
    private ListMultimap<AgencyAndId, AlertPatch> patchesByRoute = LinkedListMultimap.create();
    private ListMultimap<AgencyAndId, AlertPatch> patchesByStop = LinkedListMultimap.create();
    private ListMultimap<StopAndRouteOrTripKey, AlertPatch> patchesByStopAndRoute = LinkedListMultimap.create();
    private ListMultimap<StopAndRouteOrTripKey, AlertPatch> patchesByStopAndTrip = LinkedListMultimap.create();
    private ListMultimap<AgencyAndId, AlertPatch> patchesByTrip = LinkedListMultimap.create();
    private ListMultimap<String, AlertPatch> patchesByAgency = LinkedListMultimap.create();
    private ListMultimap<String, AlertPatch> patchesByTripPattern = LinkedListMultimap.create();

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
        List<AlertPatch> result = patchesByStop.get(stop);
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
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getRoutePatches(AgencyAndId route) {
        List<AlertPatch> result = patchesByRoute.get(route);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;

    }

    @Override
    public Collection<AlertPatch> getTripPatches(AgencyAndId trip) {
        List<AlertPatch> result = patchesByTrip.get(trip);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }


    @Override
    public Collection<AlertPatch> getAgencyPatches(String agency) {
        List<AlertPatch> result = patchesByAgency.get(agency);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndRoutePatches(AgencyAndId stop, AgencyAndId route) {
        List<AlertPatch> result = patchesByStopAndRoute.get(new StopAndRouteOrTripKey(stop, route));
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndTripPatches(AgencyAndId stop, AgencyAndId trip) {
        List<AlertPatch> result = patchesByStopAndTrip.get(new StopAndRouteOrTripKey(stop, trip));
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getTripPatternPatches(TripPattern pattern) {
        List<AlertPatch> result = patchesByTripPattern.get(pattern.code);
        if (result == null) {
            result = Collections.emptyList();
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
            patchesByStopAndTrip.put(new StopAndRouteOrTripKey(stop, trip), alertPatch);
        } else if (stop != null && route != null) {
            patchesByStopAndRoute.put(new StopAndRouteOrTripKey(stop, route), alertPatch);
        } else {
            if (stop != null) {
                patchesByStop.put(stop, alertPatch);
            }

            if (route != null) {
                patchesByRoute.put(route, alertPatch);
            }

            if (trip != null) {
                patchesByTrip.put(trip, alertPatch);
            }
        }

        String agency = alertPatch.getAgency();
        if (agency != null && !agency.isEmpty()) {
            patchesByAgency.put(agency, alertPatch);
        }

        List<TripPattern> tripPatterns = alertPatch.getTripPatterns();
        if (tripPatterns != null) {
            for (TripPattern pattern : tripPatterns) {
                patchesByTripPattern.put(pattern.code, alertPatch);
            }
        }

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
            patchesByStop.remove(stop, alertPatch);
        }
        if (route != null) {
            patchesByRoute.remove(route, alertPatch);
        }
        if (trip != null) {
            patchesByTrip.remove(trip, alertPatch);
        }

        if (stop != null && route != null) {
            patchesByStopAndRoute.remove(new StopAndRouteOrTripKey(stop, route), alertPatch);
        }
        if (stop != null && trip != null) {
            patchesByStopAndTrip.remove(new StopAndRouteOrTripKey(stop, trip), alertPatch);
        }
        String agency = alertPatch.getAgency();
        if (agency != null) {
            patchesByAgency.remove(agency, alertPatch);
        }
        List<TripPattern> tripPatterns = alertPatch.getTripPatterns();
        if (tripPatterns != null) {
            for (TripPattern pattern : tripPatterns) {
                patchesByTripPattern.remove(pattern.code, alertPatch);
            }
        }
        alertPatch.remove(graph);
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
