package org.opentripplanner.graph_builder.triptransformer;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class CsvLine {
    private final static int MAX_PRINT_SIZE = 12;
    private static final String SEP = "; ";

    private String tag;
    private String agency;
    private Route route;
    private String headsign;
    private List<Departure> departures = new ArrayList<>();
    private List<Departure> depDayBefore = new ArrayList<>();


    CsvLine(String tag, Route route, String headsign) {
        this.tag = tag;
        this.agency = route.getAgency().getName();
        this.route = route;
        this.headsign = headsign;
    }

    Route route() { return route; }
    String headsign() { return headsign; }
    String tag() { return tag; }

    void addDeparture(Trip trip, Integer tripDepartureTime, boolean oneDayService) {
        departures.add(new Departure(trip, tripDepartureTime, oneDayService));
    }

    void addDepDayBefore(Trip trip, Integer tripDepartureTime) {
        depDayBefore.add(new Departure(trip, tripDepartureTime, false));
    }

    static String csvHeader() {
        return "Tag; Agency; Route id; Public code; Route; Headsign; Dep. day before; Departures;DST Service";
    }

    private void sortDepartures() {
        Collections.sort(departures);
        Collections.sort(depDayBefore);
    }

    String toCsv(){
        sortDepartures();
        return (tag==null ? "" : tag) + SEP
                + agency + SEP
                + route.getId() + SEP
                + route.getShortName() + SEP
                + route.getLongName() + SEP
                + headsign + SEP
                + timesToString(depDayBefore) + SEP
                + timesToString(departures) + SEP
                + (dstOneDayService(departures) ? "Ok" : "");
    }

    private static boolean dstOneDayService(List<Departure> departures) {
        for (Departure it : departures) {
            if(!it.isSummerTime()) return true;
            if (!it.oneDayService) return false;
        }
        return true;
    }

    private static String timesToString(Collection<Departure> departures) {
        return departures.stream()
                .limit(MAX_PRINT_SIZE)
                .map(Departure::timeToString)
                .collect(Collectors.joining(" "))
                + more(departures);
    }

    private static String more(Collection<Departure> list) {
        final int size = list.size();
        return size > MAX_PRINT_SIZE + 2 ? " .. (" + (size - MAX_PRINT_SIZE) + " more)" : "";
    }

}
