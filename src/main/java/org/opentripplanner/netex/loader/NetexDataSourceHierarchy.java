/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/
package org.opentripplanner.netex.loader;

import org.opentripplanner.standalone.config.NetexParameters;
import org.opentripplanner.standalone.datastore.CompositeDataSource;
import org.opentripplanner.standalone.datastore.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NetexDataSourceHierarchy {
    private static final Logger LOG = LoggerFactory
            .getLogger(NetexDataSourceHierarchy.class);

    private final CompositeDataSource source;

    private final NetexParameters config;

    private final List<DataSource> sharedEntries = new ArrayList<>();

    private final Map<String, GroupEntries> groupEntries = new TreeMap<>();

    private String currentGroup = null;

    NetexDataSourceHierarchy(CompositeDataSource source, NetexParameters netexConfig) {
        this.source = source;
        this.config = netexConfig;
        distributeEntries();
    }

    Iterable<DataSource> sharedEntries() {
        return sharedEntries;
    }

    Iterable<GroupEntries> groups() {
        return groupEntries.values();
    }

    private void distributeEntries() {
        for (DataSource entry : source.content()) {
            String name = entry.name();

            if(ignoredFile(name)) {
                LOG.debug("Netex file ignored: {}.", name);
            }
            else if (isSharedFile(name)) {
                sharedEntries.add(entry);
            }
            else if (isGroupEntry(name, config.sharedGroupFilePattern)) {
                groupEntries.get(currentGroup).addSharedEntry(entry);
            }
            else if (isGroupEntry(name, config.groupFilePattern)) {
                groupEntries.get(currentGroup).addIndependentEntries(entry);
            }
            else {
                LOG.warn("Netex file ignored: {}. The file do not match file patterns.", name);
            }
        }
    }

    private boolean ignoredFile(String name) {
        return config.ignoreFilePattern.matcher(name).matches();
    }

    private boolean isSharedFile(String name) {
        return config.sharedFilePattern.matcher(name).matches();
    }

    private boolean isGroupEntry(String name, Pattern filePattern) {
        Matcher m = filePattern.matcher(name);
        if (!m.matches()) {
            return false;
        }
        try {
            currentGroup = m.group(1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException("Netex file patten '" + filePattern
                    + "' is missing a group pattern like: '(\\w+)' in '(\\w+)-.*\\.xml' ");
        }
        groupEntries.computeIfAbsent(currentGroup, GroupEntries::new);
        return true;
    }

}
