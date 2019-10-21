package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitBuilder;

import java.io.File;

public class TripTransformService {
    /**
     * <ul>
     * <li>Run Trip transformation, See {@link TripCopyAndMoveTransform}.
     * <li>And print Daylight Saving Time report. The report is enabled by adding a System property at
     * startup: {@code "-DenableDstReport"}.
     * </ul>
     */
    public static void runTripTransform(OtpTransitBuilder otpBuilder, CalendarServiceData calendarServiceData, File feedDirectory) {
        boolean enableDstReport = System.getProperty("enableDstReport") != null;
        if (enableDstReport) new PrintDaylightSavingTimeReport(otpBuilder, calendarServiceData).print("DST-before.csv");
        new TripCopyAndMoveTransform(feedDirectory, otpBuilder, calendarServiceData).run();
        if (enableDstReport) new PrintDaylightSavingTimeReport(otpBuilder, calendarServiceData).print("DST-after.csv");

    }
}
