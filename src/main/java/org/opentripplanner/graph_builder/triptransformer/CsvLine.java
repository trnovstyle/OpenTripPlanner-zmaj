package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class CsvLine {
    private static final String SEP = "; ";
    private String agency;
    private Route route;
    private String headsign;
    private List<Departure> departures = new ArrayList<>();
    private List<Departure> depDayBefore = new ArrayList<>();


    CsvLine(Route route, String headsign) {
        this.agency = route.getAgency().getName();
        this.route = route;
        this.headsign = headsign;
    }

    void addDeparture(Trip trip, Integer tripDepartureTime, boolean oneDayService) {
        departures.add(new Departure(trip, tripDepartureTime, oneDayService));
    }

    void addDepDayBefore(Trip trip, Integer tripDepartureTime) {
        depDayBefore.add(new Departure(trip, tripDepartureTime, false));
    }

    static String csvHeader() {
        return "Agency; Public code; Route; Headsign; Only DST day; Dep. day before; Departures; Route id";
    }

    String toCsv(){
        Collections.sort(departures);
        Collections.sort(depDayBefore);

        boolean oneDayService = dstOneDayService(departures);

        List<Departure> timeList = departures.stream().limit(10).collect(Collectors.toList());
        int realCutOffTime = timeList.get(timeList.size() -1).time;
        String times = timeList.stream().map(Departure::timeToString).collect(Collectors.joining(" "));
        long more = departures.stream().filter(it -> it.time > realCutOffTime).count();
        String times2 = depDayBefore.stream().map(Departure::timeToString).collect(Collectors.joining(" "));

        return agency + SEP
                + route.getShortName() + SEP
                + route.getLongName() + SEP
                + headsign + SEP +
                (oneDayService ? "" : "NO!") + SEP
                + times2 + SEP
                + times + (more > 0 ? "... (" + more + " more)" : "") + SEP
                + route.getId();
    }

    private boolean dstOneDayService(List<Departure> departures) {
        for (Departure it : departures) {
            if(!it.isSummerTime()) return true;
            if (!it.oneDayService) return false;
        }
        return true;
    }

    boolean firstDepartureIsBeforeDstChanges() {
        Collections.sort(departures);
        return TimeUtil.isDstSummerTime(departures.get(0).time);
    }
}
