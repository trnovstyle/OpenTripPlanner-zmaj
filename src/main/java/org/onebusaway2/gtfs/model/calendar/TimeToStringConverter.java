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
package org.onebusaway2.gtfs.model.calendar;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeToStringConverter {
    private static Pattern hhmmssPattern = Pattern.compile("^(-?)(\\d+):(\\d\\d):(\\d\\d)$");
    private static DecimalFormat format = new DecimalFormat("00");

    public static String toHH_MM_SS(final int seconds) {
        int absSeconds = Math.abs(seconds);


        int secondsRest = absSeconds % 60;
        int hourAndMinutes = (absSeconds - secondsRest) / 60;
        int minutes = hourAndMinutes % 60;
        int hours = (hourAndMinutes - minutes) / 60;

        StringBuilder b = new StringBuilder();

        if(seconds < 0) {
            b.append('-');
        }
        b.append(format.format(hours));
        b.append(":");
        b.append(format.format(minutes));
        b.append(":");
        b.append(format.format(secondsRest));
        return b.toString();
    }

    public static int parseHH_MM_SS(String value) {
        Matcher m = hhmmssPattern.matcher(value);
        if (!m.matches())
            throw new InvalidTimeException(value, hhmmssPattern.pattern());
        try {
            int sign = "-".equals(m.group(1)) ? -1 : 1;
            int hours = Integer.parseInt(m.group(2));
            int minutes = Integer.parseInt(m.group(3));
            int seconds = Integer.parseInt(m.group(4));

            return sign * (60 * (60 * hours + minutes) + seconds);
        } catch (NumberFormatException ex) {
            throw new InvalidTimeException(value, hhmmssPattern.pattern());
        }
    }
}