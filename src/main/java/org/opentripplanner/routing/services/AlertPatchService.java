/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.services;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.Set;

public interface AlertPatchService {
    Collection<AlertPatch> getAllAlertPatches();

    AlertPatch getPatchById(String id);

    Collection<AlertPatch> getStopPatches(AgencyAndId stop);

    Collection<AlertPatch> getRoutePatches(AgencyAndId route);

    Collection<AlertPatch> getTripPatches(AgencyAndId trip);

    Collection<AlertPatch> getAgencyPatches(String agency);

    Collection<AlertPatch> getStopAndRoutePatches(AgencyAndId stop, AgencyAndId route);

    Collection<AlertPatch> getTripPatternPatches(TripPattern tripPattern);

    void apply(AlertPatch alertPatch);

    void expire(Set<String> ids);

    void expireAll();

    void expireAllExcept(Set<String> ids);
}
