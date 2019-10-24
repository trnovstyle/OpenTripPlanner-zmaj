package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.model.calendar.ServiceDate;

import java.time.Duration;
import java.util.HashMap;


class TimeUtil {
    /** The time the clock is changed from summer time to winter time given in service wintertime (seconds). */
    private static final int DST_CHANGE_SEC = 2 * 60 * 60;
    static final ServiceDate DST_2019_OCT = new ServiceDate(2019, 10, 27);
    private static final HashMap<ServiceDate, Integer> DST_OFFSET_PREV_DAY = new HashMap<>();

    static {
        DST_OFFSET_PREV_DAY.put(DST_2019_OCT, 25 * 3600);
    }

    static boolean isDstSummerTime(int timeSec) {
        return timeSec <= DST_CHANGE_SEC;
    }

    static String dstToString(int serviceTime) {
        boolean isSummerTime = isDstSummerTime(serviceTime);
        String str = timeToString(isSummerTime ? serviceTime + 3600 : serviceTime);
        return (isSummerTime && serviceTime >= -3600) ?  str + "*" : str;
    }

    static String timeToString(int timeSec) {
        boolean neg = timeSec < 0;
        int temp = (neg ? -timeSec : timeSec) / 60;
        int hour = temp / 60;
        int min = temp % 60;

        return (neg ? "-" : "") + hour + ":" + String.format("%02d", min);
    }

    static ServiceDate dayBefore(ServiceDate date) {
        if(date.getDay() < 2) {
            throw new IllegalArgumentException(
                "Only support for finding the day before in the same month."
            );
        }
        return new ServiceDate(date.getYear(), date.getMonth(), date.getDay()-1);
    }

    static int timeInSec(String text) {
        text = text.trim();
        boolean summertime = false;
        if(text.endsWith("*")) {
            text = text.substring(0, text.length()-1);
            summertime = true;
        }
        String[] tokens = text.split(":");
        int hh = Integer.parseInt(tokens[0]);
        int mm = Integer.parseInt(tokens[1]);

        return (((summertime ? hh-1 : hh) * 60) + mm) * 60;
    }

    static int durationInSec(String text) {
        if(text.startsWith("-")) {
            text = "-PT" + text.substring(1);
        }
        else {
            text = "PT" + text;
        }
        long d = Duration.parse(text).getSeconds();

        return (int)d;
    }

    public static int offsetPrevDaySec(ServiceDate date) {
        return DST_OFFSET_PREV_DAY.getOrDefault(date, 24 * 3600);
    }
}
