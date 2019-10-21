package org.opentripplanner.graph_builder.triptransformer;

import org.apache.commons.io.FileUtils;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * This service is used to patch (copy/move) trip using a "TransitTransform.txt" file.
 * Put the file in the same directory as the GTFS or Netex Zip file and make sure the
 * GTFS/Netex module run this service after the ServiceCalendar and transit data is
 * loaded.
 * <p>
 * The "TransitTransform.txt" should be UTF-8 encoded and use the following format:
 * <p>
 * Transform transit timetable using some simple commands
 * <p>
 * <p>
 * COMMANDS
 * <ul>
 * <li>{@code applyOnDate; [yyyy-mm-dd]} All commands after this line will apply the command on trips found 
 * on this DATE. This command must be run at least once, and before any copy/move command.
 * <li>{@code moveToServiceId; [Service ID]} All commands will assign all copied or moved trips to this serviceId 
 * (Netex dayType). This command must be run at least once, and before any copy/move command.
 * <li>{@code CopyFromTo; [Route ID]; [Headsign]; [Source time]; [Target time]} Copy a trip with the given 
 * [Route ID], [Headsign] and/or [source time] to the given [target time]. All stop times are moved with the same
 * delta as the depature time.
 * <li>{@code CopyTimeShift; [Route ID]; [Headsign]; [Time shift]; [List of source times]} Copy a trip with the 
 * to the given [target time]. All stop times are moved with the same delta as the depature time.
 * <li>{@code MoveTimeShift; [Route ID]; [Headsign]; [Time shift]; [List of source times]} Copy a trip with the 
 * given [Route ID], [Headsign] and/or [source time] to the given [target time]. All stop times are moved with 
 * the same delta as the depature time.
 * <p>
 * <p>
 * ARGUMENTS
 * <ul>
 * <li>{@code [yyyy-mm-dd]} Service date in ISO8601 format, using the feed timezone.
 * <li>{@code [Service ID]}  Service ID, must exist after the transit data is imported into OTP, PS!This ID is
 * generated for Nexet feeds.
 * <li>{@code [Route ID]}    The Line ID (Netex) or Route ID (GTFS) witch the trip (source) is part of. Without
 * feed id prefix.
 * <li>{@code [Headsign]}    The headsign (GTFS) or DestinationDisplay (Netex) for the trip used. If a '*' is
 * used ALL trips are selected.
 * <li>{@code [Source time]} The source trip depature time used to find the correct trip. If a '*' is used ALL
 * trip depatures are accepted.
 * <li>{@code [Target time]} The new or moved trip will have this time as its first depature time, all other
 * stop times are adjusted with the same delta.
 * <li>{@code [Time shift]}  A duration that specify how mutch to change the trip stop times. It can be negative
 * an is specified like this:
 *     <p>{@code 1h2m} is 1 hour and 2 minutes
 *     <p>{@code -7m} is minus 7 minutes
 * <li>{@code [List of source times]} A list of source times seperated by ';'.
 * </ul>
 * <p>
 *  The commands are not case sensitive and the 'applyOnDate' and 'moveToServiceId' only affect the commands following
 *  them until the same command
 *  ('applyOnDate' or 'moveToServiceId') is repeted.
 */
public class TripCopyAndMoveTransform {
    private static final Logger LOG = LoggerFactory.getLogger(TripCopyAndMoveTransform.class);

    private final TransitServiceDecorator transit;
    private final File inputFile;
    private String currentLine;
    private AgencyAndId currentServiceIdTarget = null;
    private ServiceDate currentServiceDate = null;


    public TripCopyAndMoveTransform(File inputDir, OtpTransitBuilder transitService, CalendarServiceData data) {
        if(inputDir == null) {
            this.inputFile = null;
            this.transit = null;
        }
        else {
            this.transit = new TransitServiceDecorator(transitService, data);
            this.inputFile = new File(inputDir, "TransitTransform.txt");
        }
    }

    public void run() {
        try {
            if(inputFile == null || !inputFile.exists() || !inputFile.canRead()) {
                LOG.info("Trip Transit transformations no performed, no file available.");
                return;
            }

            List<String> lines = FileUtils.readLines(inputFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if(line.isBlank() || line.startsWith("#")) continue;
                processLine(line);
            }
        }
        catch (IOException e) {
            LOG.error("Unable to read input file: " + inputFile, e);
        }
        catch (Exception e) {
            LOG.error("Unable to process input: {}" + currentLine, e);
        }
    }

    private void processLine(String line) {
        this.currentLine = line;
        List<String> tokens = Arrays.stream(line.split(";")).map(String::trim).collect(Collectors.toList());

        if(tokens.size() >= 2) {
            executeCommand(tokens);
        }
        else {
            parseError();
        }
    }

    private void executeCommand(List<String> tokens) {
        Command cmd = parseCommand(tokens.get(0));
        String arg1 = tokens.get(1);
        List<String> args = tokens.subList(2, tokens.size());

        switch (cmd) {
            case CopyFromTo:
                copyFromTo(createId(arg1), args);
                break;
            case CopyTimeShift:
                copyTimeShift(createId(arg1), args);
                break;
            case MoveTimeShift:
                moveTimeShift(createId(arg1), args);
                break;
            case ApplyOnDate:
                // applyOnDate; 2019-10-27
                currentServiceDate = parseServiceDate(arg1);
                break;
            case MoveToServiceId:
                // moveToServiceId; RUT:DayType:0-133916
                currentServiceIdTarget = createId(arg1);
                break;
            default:
                parseError();
                break;
        }
    }

    /** CopyFromTo;RUT:Line:20;Skøyen;2:02*;1:02* */
    private void copyFromTo(AgencyAndId lineId, List<String> args) {
        if(!verifyInputIsOk(args, 3, 3)) return;
        Predicate<String> headsignFilter = headsignFilter(args.get(0));
        int sourceDepTimeSec = TimeUtil.timeInSec(args.get(1));
        int targetDepTimeSec = TimeUtil.timeInSec(args.get(2));
        int timeShiftSec = targetDepTimeSec - sourceDepTimeSec;
        Trip trip = transit.findTrip(currentServiceDate, lineId, headsignFilter, t -> t == sourceDepTimeSec);
        transit.copyTrip(trip, timeShiftSec, currentServiceIdTarget);
        success();
    }


    /** CopyTimeShift;RUT:Line:20;Helsfyr;-1h;2:20*;2:50* */
    private void copyTimeShift(AgencyAndId lineId, List<String> args) {
        if(!verifyInputIsOk(args, 3, 13)) return;
        Predicate<String> headsignFilter = headsignFilter(args.get(0));
        int timeshiftSec = TimeUtil.durationInSec(args.get(1));
        IntPredicate departureFilter = departureFilter(args.subList(2, args.size()));
        List<Trip> trips = transit.findAllTrips(currentServiceDate, lineId, headsignFilter, departureFilter);
        for (Trip trip : trips) {
            transit.copyTrip(trip, timeshiftSec, currentServiceIdTarget);
        }
        success();
    }
    /** MoveTimeShift;RUT:Line:3250;*;-1h;* */
    private void moveTimeShift(AgencyAndId lineId, List<String> args) {
        if(!verifyInputIsOk(args, 3, 13)) return;
        Predicate<String> headsignFilter = headsignFilter(args.get(0));
        int timeshiftSec = TimeUtil.durationInSec(args.get(1));
        IntPredicate departureFilter = departureFilter(args.subList(2, args.size()));
        List<Trip> trips = transit.findAllTrips(currentServiceDate, lineId, headsignFilter, departureFilter);
        for (Trip trip : trips) {
            transit.moveTrip(trip, timeshiftSec, currentServiceIdTarget);
        }
        success();
    }

    private IntPredicate departureFilter(List<String> args) {
        if(args.size() == 1 && "*".equals(args.get(0))) {
            return null;
        }
        final int[] times = args.stream().mapToInt(TimeUtil::timeInSec).toArray();

        return (t) -> {
            for (int time : times) {
                if (t == time) return true;
            }
            return false;
        };
    }

    private Predicate<String> headsignFilter(String arg) {
        return "*".equals(arg) ? null : arg::equals;
    }

    private static AgencyAndId createId(String text) {
        return new AgencyAndId("RB", text);
    }

    private static ServiceDate parseServiceDate(String text) {
        String[] tokens = text.split("-");
        int year = parseInt(tokens[0]);
        int month = parseInt(tokens[1]);
        int day = parseInt(tokens[2]);
        return new ServiceDate(year, month, day);
    }

    private boolean verifyInputIsOk(List<String> args, int minLimitArgs, int maxLimitArgs) {
        if(args.size() < minLimitArgs || args.size() > maxLimitArgs) {
            parseError();
            return false;
        }

        if(currentServiceDate == null || currentServiceIdTarget == null) {
            LOG.error(
                    "Transit transformation line skipped. ApplyOnDate: {}, moveToServiceId; {}, Line: {}",
                    currentServiceDate, currentServiceIdTarget, currentLine
            );
            return false;
        }
        return true;
    }

    private void parseError() {
        LOG.error("Transit transformation parsing error: '{}'", currentLine);
    }

    private void success() {
        LOG.info("Transit transformation ok: '{}'", currentLine);
    }

    private static Command parseCommand(String value) {
        for (Command command : Command.values()) {
            if(command.name().equalsIgnoreCase(value)) return command;
        }
        return null;
    }

    private static int parseInt(String text) {
        return Integer.parseInt(text.trim());
    }

    enum Command {
        CopyFromTo, CopyTimeShift, MoveTimeShift, ApplyOnDate, MoveToServiceId;
    }
}
