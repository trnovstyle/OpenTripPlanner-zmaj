/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for Norwegian roads. 
 * The main difference compared to the default property set is that most of the highway=trunk roads also allows walking and biking, 
 * where as some does not. 
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 * 
 *   
 * @author seime
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class NorwayWayPropertySetSource implements WayPropertySetSource {

	@Override
	public void populateProperties(WayPropertySet props) {
        // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
        props.setProperties("highway=trunk_link", StreetTraversalPermission.ALL, 2.06,
                2.06);
        props.setProperties("highway=trunk", StreetTraversalPermission.ALL, 7.47, 7.47);
        
        // Don't recommend walking in trunk road tunnels (although actually legal unless explicitly forbidden)
        props.setProperties("highway=trunk;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);
        props.setProperties("highway=trunk_link;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

        // Do not walk on "Motortrafikkvei" ("motorvei klasse b")
        props.setProperties("motorroad=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

        // Allow bikes on sidewalks
        props.setProperties("footway=sidewalk;highway=footway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        /*
         * Automobile speeds in Norway. General speed limit is 80kph unless signs says otherwise
         * 
         */
        /*
        props.setCarSpeed("highway=motorway", 22.22f); // 80 km/t
        props.setCarSpeed("highway=motorway_link", 22.22f); // 80 km/t
        props.setCarSpeed("highway=trunk", 22.22f); // 80 km/t
        props.setCarSpeed("highway=trunk_link", 22.22f); // 80 km/t
        props.setCarSpeed("highway=primary", 22.22f); // 80 km/t
        props.setCarSpeed("highway=primary_link", 22.22f); // 80 km/t
        props.setCarSpeed("highway=secondary", 22.22f); // 80 km/t
        props.setCarSpeed("highway=secondary_link", 22.22f); // 80 km/t
        props.setCarSpeed("highway=tertiary", 22.22f); // 80 km/t
        props.setCarSpeed("highway=tertiary_link", 22.22f); // 80 km/t
        props.setCarSpeed("highway=living_street", 1.94f); // 7 km/t

        props.setCarSpeed("highway=pedestrian", 1.94f); // 7 km/t

        props.setCarSpeed("highway=residential", 8.33f); // 30 km/t
        props.setCarSpeed("highway=unclassified", 22.22f); // 80 km/t
        props.setCarSpeed("highway=service", 13.89f); // 50 km/t
        props.setCarSpeed("highway=track", 22.22f); // 30 km/t
        props.setCarSpeed("highway=road", 13.89f); // 50 km/t
        */

        props.defaultSpeed = 22.22f; // 80
		
        // Read the rest from the default set
		new DefaultWayPropertySetSource().populateProperties(props);
	}
}
