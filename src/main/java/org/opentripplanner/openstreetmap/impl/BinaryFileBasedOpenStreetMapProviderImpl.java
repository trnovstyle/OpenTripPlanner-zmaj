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

package org.opentripplanner.openstreetmap.impl;

import crosby.binary.file.BlockInputStream;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.standalone.datastore.DataSource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes: First the relations, then
 * the ways, then the nodes are also loaded.
 *
 * @see http://wiki.openstreetmap.org/wiki/PBF_Format
 * @see OpenStreetMapContentHandler#biPhase
 * @since 0.4
 */
public class BinaryFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private DataSource source;

    public BinaryFileBasedOpenStreetMapProviderImpl(DataSource source) {
        this.source = source;
    }

    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            BinaryOpenStreetMapParser parser = new BinaryOpenStreetMapParser(handler);
            parseIteration(parser, 1);
            handler.doneFirstPhaseRelations();

            parseIteration(parser, 2);
            handler.doneSecondPhaseWays();

            parseIteration(parser, 3);
            handler.doneThirdPhaseNodes();
        }
        catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + source.path(), ex);
        }
    }

    private void parseIteration(BinaryOpenStreetMapParser parser, int iteration) throws IOException {
        parser.setParseRelations(iteration == 1);
        parser.setParseWays(iteration == 2);
        parser.setParseNodes(iteration == 3);
        try (InputStream in = source.asInputStream()) {
            new BlockInputStream(in, parser).process();
        }
    }

    @Override
    public void checkInputs() { }

    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + source.path() + ")";
    }
}
