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
package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Pattern;

public class NetexParameters {

    private static final String EMPTY_STRING_PATTERN = "$^";

    private static final String MODULE_FILE_PATTERN = ".*-netex-no\\.zip";

    private static final String IGNORE_FILE_PATTERN = EMPTY_STRING_PATTERN;

    private static final String SHARED_FILE_PATTERN = "shared-data\\.xml";

    private static final String SHARED_GROUP_FILE_PATTERN = "(\\w{3})-.*-shared\\.xml";

    private static final String GROUP_FILE_PATTERN = "(\\w{3})-.*\\.xml";

    private static final String NETEX_FEED_ID = "DefaultFeed";

    private static final String DEFAULT_FLEX_MAX_TRAVEL_TIME = "1t+2";

    private static final int DEFAULT_MINIMUM_FLEX_PADDING_TIME = 15;

    /**
     * This field is used to identify the specific NeTEx feed. It is used instead of the feed_id field in GTFS file
     * feed_info.txt.
     */

    public final String netexFeedId;

    /**
     * This field is used to exclude matching <em>files</em> in the module file(zip file entries).
     * The <em>ignored</em> files are <em>not</em> loaded.
     * <p>
     * Default value is <code>'$^'</code> witch matches empty stings (not a valid file name).
     * @see #sharedFilePattern
     */
    public final Pattern ignoreFilePattern;

    /**
     * This field is used to match <em>shared files</em>(zip file entries) in the module file.
     * Shared files are loaded first. Then the rest of the files are grouped and loaded.
     * <p>
     * The pattern <code>'shared-data\.xml'</code> matches <code>'shared-data.xml'</code>
     * <p>
     * File names are matched in the following order - and treated accordingly to the first match:
     * <ol>
     *     <li>{@link #ignoreFilePattern}</li>
     *     <li>Shared file pattern (this)</li>
     *     <li>{@link #sharedGroupFilePattern}.</li>
     *     <li>{@link #groupFilePattern}.</li>
     * </ol>
     * <p>
     * Default value is <code>'shared-data\.xml'</code>
     */
    public final Pattern sharedFilePattern;

    /**
     * This field is used to match <em>shared group files</em> in the module file(zip file entries).
     * Typically this is used to group all files from one agency together.
     * <p>
     * <em>Shared group files</em> are loaded after shared files, but before the matching group
     * files. Each <em>group</em> of files are loaded as a unit, followed by next group.
     * <p>
     * Files are grouped together by the first group pattern in the regular expression.
     * <p>
     * The pattern <code>'(\w{3})-.*-shared\.xml'</code> matches <code>'RUT-shared.xml'</code> with
     * group <code>'RUT'</code>.
     * <p>
     * Default value is <code>'(\w{3})-.*-shared\.xml'</code>
     * @see #sharedFilePattern
     * @see #groupFilePattern
     */
    public final Pattern sharedGroupFilePattern;

    /**
     * This field is used to match <em>group files</em> in the module file(zip file entries).
     * <em>group files</em> are loaded right the after <em>shared group files</em> are loaded.
     * <p>
     * Files are grouped together by the first group pattern in the regular expression.
     * <p>
     * The pattern <code>'(\w{3})-.*\.xml'</code> matches <code>'RUT-Line-208-Hagalia-Nevlunghavn.xml'</code> with
     * group <code>'RUT'</code>.
     * <p>
     * Default value is <code>'(\w{3})-.*\.xml'</code>
     * @see #sharedFilePattern
     * @see #sharedGroupFilePattern
     */
    public final Pattern groupFilePattern;

    /**
     * This is the default value for how to calculate travel time for call-and-ride trips when a value is not given in
     * the NeTEx dataset. Travel time is expressed on the form <code>xt+c</code> where t is the direct travel time
     * calculated by the travel planner and x and c are constants.
     * Default value is <code>2t+10</code>.
     */

    public final String defaultFlexMaxTravelTime;

    /**
     * This is the default value for padding the start time of a flexible trip in relation to the
     * current system time. We are keeping this parameter separate from minimumBookingPeriod. As
     * a consequence this concept is not currently represented in NeTEx.
     */

    public final int defaultMinimumFlexPaddingTimeMins;

    NetexParameters(JsonNode config) {
        ignoreFilePattern = pattern("ignoreFilePattern", IGNORE_FILE_PATTERN, config);
        sharedFilePattern = pattern("sharedFilePattern", SHARED_FILE_PATTERN, config);
        sharedGroupFilePattern = pattern("sharedGroupFilePattern", SHARED_GROUP_FILE_PATTERN, config);
        groupFilePattern = pattern("groupFilePattern", GROUP_FILE_PATTERN, config);
        netexFeedId = text("netexFeedId", NETEX_FEED_ID, config);
        defaultFlexMaxTravelTime = text("flexMaxTravelTime", DEFAULT_FLEX_MAX_TRAVEL_TIME, config);
        defaultMinimumFlexPaddingTimeMins = integer("defaultMinimumFlexPaddingTime", DEFAULT_MINIMUM_FLEX_PADDING_TIME, config);
    }

    private static Pattern pattern(String path, String defaultValue, JsonNode config) {
        return Pattern.compile(text(path, defaultValue, config));
    }

    private static String text(String path, String defaultValue, JsonNode config) {
        return config == null ? defaultValue : config.path(path).asText(defaultValue);
    }

    private static int integer(String path, int defaultValue, JsonNode config) {
        return config == null ? defaultValue : config.path(path).asInt(defaultValue);
    }
}
