package org.opentripplanner.graph_builder.triptransformer;

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
    public static void runTripTransform(OtpTransitBuilder otpBuilder, File feedDirectory) {
        boolean generateCmds = System.getProperty("generateCmds") != null;
        boolean printReport = System.getProperty("enableDstReport") != null;

        PrintDaylightSavingTimeReport report = null;
        if (printReport) {
            report = new PrintDaylightSavingTimeReport(otpBuilder);
            report.collectData("IN -->");
            System.out.println("\n\n\n* * * * * * * * * * * * * * * * * * * * * * * *");
        }
        if(generateCmds) {
            new GenerateTripCopyAndMoveTransforms(otpBuilder).generateCmds();
        }
        else {
            new TripCopyAndMoveTransform(feedDirectory, otpBuilder).run();
            if (report != null) report.collectData("OUT <-");
        }
        if (report != null) {
            report.print(null);
            report.print("DST-Summary.csv");
        }
        if(printReport) System.out.println("* * * * * * * * * * * * * * * * * * * * * * * *\n\n\n");
    }
}
